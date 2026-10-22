package az.cyberlaw.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import az.cyberlaw.chat.ConversationDtos.ConversationDetailDto;
import az.cyberlaw.chat.ConversationDtos.ConversationDto;
import az.cyberlaw.chat.ConversationDtos.MessageDto;
import az.cyberlaw.chat.ConversationDtos.SourceDto;
import az.cyberlaw.chat.ConversationMessage.Role;
import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.document.DocumentDeleted;
import az.cyberlaw.document.DocumentScope;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved chats. A question and its answer are saved together once the answer is complete, so a failed or
 * stopped answer leaves nothing half-written behind. They also give the model its conversation memory.
 */
@Service
public class ChatHistoryService {

	private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

	private static final int TITLE_LENGTH = 60;

	private static final int MAX_LISTED = 200;

	private final ConversationRepository conversations;

	private final ConversationMessageRepository messages;

	private final MessageSourceRepository sources;

	private final CyberLawProperties props;

	public ChatHistoryService(ConversationRepository conversations, ConversationMessageRepository messages,
			MessageSourceRepository sources, CyberLawProperties props) {
		this.conversations = conversations;
		this.messages = messages;
		this.sources = sources;
		this.props = props;
	}

	public List<ConversationDto> list(UUID ownerId) {
		return conversations.findByOwnerIdOrderByUpdatedAtDesc(ownerId, PageRequest.of(0, MAX_LISTED))
			.stream()
			.map(ConversationDto::of)
			.toList();
	}

	public ConversationDetailDto get(UUID ownerId, UUID id) {
		Conversation conversation = owned(ownerId, id);
		List<ConversationMessage> all = messages.findByConversationIdOrderByPosition(id);
		Map<UUID, List<SourceDto>> cited = sources
			.findByMessageIdInOrderByPosition(all.stream().map(ConversationMessage::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(MessageSource::getMessageId,
					Collectors.mapping(SourceDto::of, Collectors.toList())));
		List<MessageDto> dtos = all.stream()
			.map(m -> new MessageDto(m.getId().toString(), m.getRole() == Role.USER ? "user" : "assistant",
					m.getContent(), cited.getOrDefault(m.getId(), List.of()), m.getCreatedAt()))
			.toList();
		return new ConversationDetailDto(ConversationDto.of(conversation), dtos);
	}

	@Transactional
	public ConversationDto rename(UUID ownerId, UUID id, String title) {
		Conversation conversation = owned(ownerId, id);
		conversation.rename(title.strip());
		return ConversationDto.of(conversations.save(conversation));
	}

	@Transactional
	public void delete(UUID ownerId, UUID id) {
		conversations.delete(owned(ownerId, id));
	}

	@Transactional
	public void deleteAll(UUID ownerId) {
		conversations.deleteByOwner(ownerId);
	}

	/** Throws CONVERSATION_NOT_FOUND for a chat that does not exist or belongs to someone else. */
	public Conversation owned(UUID ownerId, UUID id) {
		return conversations.findById(id)
			.filter(c -> c.getOwnerId().equals(ownerId))
			.orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));
	}

	/** The last messages of the chat, oldest first: the model's memory of the conversation. */
	public List<Message> recentMessages(UUID conversationId, int max) {
		List<ConversationMessage> newest = new ArrayList<>(
				messages.findByConversationIdOrderByPositionDesc(conversationId, PageRequest.of(0, max)));
		Collections.reverse(newest);
		return newest.stream()
			.map(m -> m.getRole() == Role.USER ? (Message) new UserMessage(m.getContent())
					: new AssistantMessage(m.getContent()))
			.toList();
	}

	/**
	 * Saves a question and its finished answer, with the passages the answer cites. A new chat (null id) is
	 * created here, named after its first question.
	 */
	@Transactional
	public ConversationDto saveExchange(UUID ownerId, UUID conversationId, String question, String answer,
			List<Source> retrieved, SearchScope scope) {
		Conversation conversation = conversationId != null ? owned(ownerId, conversationId)
				: conversations.save(new Conversation(ownerId, title(question), scope));
		int next = (int) messages.countByConversationId(conversation.getId());
		messages.save(new ConversationMessage(conversation.getId(), next, Role.USER, question));
		ConversationMessage reply = messages
			.save(new ConversationMessage(conversation.getId(), next + 1, Role.ASSISTANT, answer));
		Set<Integer> cited = Citations.cited(answer);
		sources.saveAll(retrieved.stream()
			.filter(s -> cited.contains(s.index()))
			.map(s -> new MessageSource(reply.getId(), s))
			.toList());
		conversation.touch(scope);
		return ConversationDto.of(conversations.save(conversation));
	}

	/** "Regenerate": removes the last question and answer if they belong to this question. */
	@Transactional
	public void forgetLastExchange(UUID conversationId, String question) {
		List<ConversationMessage> lastTwo = messages.findByConversationIdOrderByPositionDesc(conversationId,
				PageRequest.of(0, 2));
		if (lastTwo.size() == 2 && lastTwo.get(0).getRole() == Role.ASSISTANT
				&& lastTwo.get(1).getRole() == Role.USER && lastTwo.get(1).getContent().equals(question)) {
			messages.deleteAll(lastTwo);
		}
	}

	/**
	 * Private documents are deleted after a few days, and their text must not live on in old answers: the
	 * copied passages lose their text (their title and article stay). Official laws are kept as they were.
	 */
	@EventListener
	@Transactional
	public void onDocumentDeleted(DocumentDeleted event) {
		if (event.scope() == DocumentScope.PRIVATE) {
			sources.forgetText(event.documentId());
		}
	}

	/** Only when CHAT_HISTORY_DAYS is set: by default chats are kept until the user deletes them. */
	@Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
	@Transactional
	public void deleteOldChats() {
		int days = props.chat().historyDays();
		if (days <= 0) {
			return;
		}
		int deleted = conversations.deleteNotUpdatedSince(Instant.now().minus(Duration.ofDays(days)));
		if (deleted > 0) {
			log.info("Deleted {} chat(s) without activity for {} days.", deleted, days);
		}
	}

	/** The first question, cut at a word to about 60 characters. */
	static String title(String question) {
		String text = question.strip().replaceAll("\\s+", " ");
		if (text.length() <= TITLE_LENGTH) {
			return text;
		}
		int cut = text.lastIndexOf(' ', TITLE_LENGTH);
		return text.substring(0, cut > TITLE_LENGTH / 2 ? cut : TITLE_LENGTH) + "…";
	}

}
