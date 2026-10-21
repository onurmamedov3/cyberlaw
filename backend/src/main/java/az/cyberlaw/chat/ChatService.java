package az.cyberlaw.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.document.LanguageDetector;
import az.cyberlaw.rag.RetrievalService;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

/**
 * The RAG pipeline: retrieve relevant chunks → build a grounded prompt → stream Gemini's answer.
 * Emits SSE events: "sources" (once), "token" (many), then "done" or "error".
 */
@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	public static final int MAX_QUESTION = 2000;

	private static final int FOLLOW_UP_MAX_WORDS = 6;

	/** "[2]" as written by the model (the same pattern as citedIndexes() in the frontend). */
	private static final String CITATION = "\\s*\\[\\d{1,2}\\](?!\\()";

	private final ChatModel chatModel;

	private final ChatMemory chatMemory;

	private final RetrievalService retrieval;

	private final CyberLawProperties props;

	public ChatService(ChatModel chatModel, ChatMemory chatMemory, RetrievalService retrieval,
			CyberLawProperties props) {
		this.chatModel = chatModel;
		this.chatMemory = chatMemory;
		this.retrieval = retrieval;
		this.props = props;
	}

	/**
	 * regenerate: write a new answer to the last question, replacing the previous one. It is a Boolean, not a
	 * boolean, so it may be left out: Jackson 3 rejects a missing primitive.
	 */
	public record ChatRequest(@NotBlank @Size(max = MAX_QUESTION) String question,
			@NotNull @Pattern(regexp = "[A-Za-z0-9-]{1,64}") String conversationId, SearchScope scope,
			String language, Boolean regenerate) {
	}

	public Flux<ServerSentEvent<Object>> stream(UUID userId, ChatRequest request) {
		if (!props.aiConfigured()) {
			return Flux.just(aiError(AiErrorCode.NOT_CONFIGURED));
		}
		String question = request.question().strip();
		String memoryKey = userId + ":" + request.conversationId();
		String answerLanguage = LanguageDetector.detect(question).equals("az") ? "az"
				: ("az".equals(request.language()) && !looksEnglish(question) ? "az" : "en");

		boolean smallTalk = SmallTalk.matches(question);

		return Flux.defer(() -> {
			if (Boolean.TRUE.equals(request.regenerate())) {
				forgetLastAnswer(memoryKey, question);
			}
			List<Message> history = chatMemory.get(memoryKey);
			List<Source> sources = smallTalk ? List.of()
					: retrieval.search(searchQuery(question, history), userId,
							request.scope() != null ? request.scope() : SearchScope.BOTH);
			ServerSentEvent<Object> sourcesEvent = event("sources", sources);

			if (!smallTalk && sources.isEmpty() && history.isEmpty()) {
				String answer = Prompts.noSourcesAnswer(answerLanguage);
				return Flux.just(sourcesEvent, event("token", Map.of("text", answer)), event("done", Map.of()));
			}

			List<Message> messages = new ArrayList<>();
			messages.add(new SystemMessage(smallTalk ? Prompts.smallTalkSystem(request.language())
					: Prompts.chatSystem(sources, request.language())));
			messages.addAll(withoutCitations(history));
			messages.add(new UserMessage(question));

			StringBuilder answer = new StringBuilder();
			Flux<ServerSentEvent<Object>> tokens = chatModel.stream(new Prompt(messages))
				.map(ChatService::textOf)
				.filter(text -> !text.isEmpty())
				.doOnNext(answer::append)
				.map(text -> event("token", Map.of("text", text)))
				.doOnComplete(() -> chatMemory.add(memoryKey,
						List.of(new UserMessage(question), new AssistantMessage(answer.toString()))));

			return Flux.concat(Flux.just(sourcesEvent), tokens, Flux.just(event("done", Map.of())));
		}).subscribeOn(Schedulers.boundedElastic()).onErrorResume(ex -> Flux.just(errorEvent(ex)));
	}

	public void clear(UUID userId, String conversationId) {
		chatMemory.clear(userId + ":" + conversationId);
	}

	/** Removes the last question and answer, if they are this question's, so a new answer replaces them. */
	private void forgetLastAnswer(String memoryKey, String question) {
		List<Message> history = chatMemory.get(memoryKey);
		int size = history.size();
		if (size >= 2 && history.get(size - 2) instanceof UserMessage last && question.equals(last.getText())
				&& history.get(size - 1) instanceof AssistantMessage) {
			List<Message> earlier = List.copyOf(history.subList(0, size - 2));
			chatMemory.clear(memoryKey);
			if (!earlier.isEmpty()) {
				chatMemory.add(memoryKey, earlier);
			}
		}
	}

	/**
	 * A short follow-up such as "and the penalties?" means little on its own, so it is searched together
	 * with the previous question. Longer questions are searched as they are.
	 */
	static String searchQuery(String question, List<Message> history) {
		if (question.split("\\s+").length > FOLLOW_UP_MAX_WORDS) {
			return question;
		}
		for (int i = history.size() - 1; i >= 0; i--) {
			if (history.get(i) instanceof UserMessage previous) {
				return previous.getText() + "\n" + question;
			}
		}
		return question;
	}

	/**
	 * Earlier answers cite earlier sources. Their [1], [2] would now point to the new sources, which use the
	 * same numbers, so the markers are removed from the history sent to Gemini.
	 */
	static List<Message> withoutCitations(List<Message> history) {
		return history.stream()
			.map(m -> m instanceof AssistantMessage answer
					? new AssistantMessage(answer.getText().replaceAll(CITATION, "")) : m)
			.toList();
	}

	private static String textOf(ChatResponse response) {
		if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
			return "";
		}
		String text = response.getResult().getOutput().getText();
		return text != null ? text : "";
	}

	/** Short questions such as "What is GDPR?" have no Azerbaijani letters but are clearly English. */
	private static boolean looksEnglish(String question) {
		String q = " " + question.toLowerCase() + " ";
		for (String w : List.of(" what ", " how ", " which ", " who ", " when ", " does ", " is ", " are ", " the ",
				" should ", " must ", " can ")) {
			if (q.contains(w)) {
				return true;
			}
		}
		return false;
	}

	private static ServerSentEvent<Object> event(String name, Object data) {
		return ServerSentEvent.builder(data).event(name).build();
	}

	private static ServerSentEvent<Object> errorEvent(Throwable ex) {
		if (!AiErrorCode.isAiError(ex)) {
			log.error("Chat failed", ex);
			return event("error", Map.of("code", ErrorCode.INTERNAL.name(), "message", ErrorCode.INTERNAL.message()));
		}
		AiErrorCode code = AiErrorCode.classify(ex);
		log.warn("Chat failed ({}): {}", code, ex.getMessage());
		return aiError(code);
	}

	private static ServerSentEvent<Object> aiError(AiErrorCode code) {
		return event("error", Map.of("code", code.name(), "message", AiException.describe(code)));
	}

}
