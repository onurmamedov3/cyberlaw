package az.cyberlaw.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.document.LanguageDetector;
import az.cyberlaw.rag.RetrievalService;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
 * The RAG pipeline: retrieve relevant chunks → build a grounded prompt → stream Gemini's answer → save it.
 * Emits SSE events: "sources" (once), "token" (many), "conversation" (the saved chat), then "done" or "error".
 */
@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	public static final int MAX_QUESTION = 2000;

	private static final int FOLLOW_UP_MAX_WORDS = 6;

	/** Gemini finish reasons that mean the answer was blocked by a content filter. */
	private static final List<String> SAFETY_FINISH_REASONS = List.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST",
			"SPII");

	private final ChatModel chatModel;

	private final ChatHistoryService history;

	private final RetrievalService retrieval;

	private final CyberLawProperties props;

	public ChatService(ChatModel chatModel, ChatHistoryService history, RetrievalService retrieval,
			CyberLawProperties props) {
		this.chatModel = chatModel;
		this.history = history;
		this.retrieval = retrieval;
		this.props = props;
	}

	/**
	 * conversationId: null starts a new chat. regenerate: write a new answer to the last question, replacing
	 * the previous one; a Boolean, not a boolean, so it may be left out (Jackson 3 rejects a missing primitive).
	 */
	public record ChatRequest(@NotBlank @Size(max = MAX_QUESTION) String question, UUID conversationId,
			SearchScope scope, String language, Boolean regenerate) {
	}

	public Flux<ServerSentEvent<Object>> stream(UUID userId, ChatRequest request) {
		if (!props.aiConfigured()) {
			return Flux.just(aiError(AiErrorCode.NOT_CONFIGURED));
		}
		UUID conversationId = request.conversationId();
		if (conversationId != null) {
			history.owned(userId, conversationId); // someone else's chat: 404 before anything is streamed
		}
		String question = request.question().strip();
		SearchScope scope = request.scope() != null ? request.scope() : SearchScope.BOTH;
		String answerLanguage = LanguageDetector.detect(question).equals("az") ? "az"
				: ("az".equals(request.language()) && !looksEnglish(question) ? "az" : "en");

		boolean smallTalk = SmallTalk.matches(question);

		return Flux.defer(() -> {
			if (conversationId != null && Boolean.TRUE.equals(request.regenerate())) {
				history.forgetLastExchange(conversationId, question);
			}
			List<Message> past = conversationId != null
					? history.recentMessages(conversationId, props.rag().historyMessages()) : List.of();
			List<Source> sources = smallTalk ? List.of()
					: retrieval.search(searchQuery(question, past), userId, scope);

			Flux<String> answerParts;
			if (!smallTalk && sources.isEmpty() && past.isEmpty()) {
				answerParts = Flux.just(Prompts.noSourcesAnswer(answerLanguage));
			}
			else {
				List<Message> messages = new ArrayList<>();
				messages.add(new SystemMessage(smallTalk ? Prompts.smallTalkSystem(request.language())
						: Prompts.chatSystem(sources, request.language())));
				messages.addAll(withoutCitations(past));
				messages.add(new UserMessage(question));
				answerParts = guardSafetyBlocks(chatModel.stream(new Prompt(messages)));
			}

			StringBuilder answer = new StringBuilder();
			Flux<ServerSentEvent<Object>> tokens = answerParts.doOnNext(answer::append)
				.map(text -> event("token", Map.of("text", text)));
			// runs only once the whole answer has arrived: a failed or stopped answer is not saved
			Mono<ServerSentEvent<Object>> saved = Mono.fromCallable(() -> event("conversation",
					history.saveExchange(userId, conversationId, question, answer.toString(), sources, scope)));

			return Flux.concat(Flux.just(event("sources", sources)), tokens, saved, Flux.just(event("done", Map.of())));
		}).subscribeOn(Schedulers.boundedElastic()).onErrorResume(ex -> Flux.just(errorEvent(ex)));
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
			.map(m -> m instanceof AssistantMessage answer ? new AssistantMessage(Citations.strip(answer.getText()))
					: m)
			.toList();
	}

	/**
	 * The answer text, failing with BLOCKED when Google's safety filter stopped it. Spring AI 2.0.1 does not report
	 * a block clearly: the last chunk has a SAFETY-like finish reason, or a chunk has no content at all and Spring AI
	 * fails with NoSuchElementException, or the answer simply comes back empty.
	 */
	static Flux<String> guardSafetyBlocks(Flux<ChatResponse> responses) {
		AtomicBoolean blocked = new AtomicBoolean();
		AtomicBoolean anyText = new AtomicBoolean();
		return responses.doOnNext(response -> {
			if (blockedBySafety(response)) {
				blocked.set(true);
			}
		})
			.map(ChatService::textOf)
			.filter(text -> !text.isEmpty())
			.doOnNext(text -> anyText.set(true))
			.onErrorMap(NoSuchElementException.class, ChatService::blockedError)
			.concatWith(Mono.defer(() -> blocked.get() || !anyText.get() ? Mono.error(blockedError(null)) : Mono.empty()));
	}

	private static boolean blockedBySafety(ChatResponse response) {
		if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
			return false;
		}
		String reason = String.valueOf(response.getResult().getMetadata().getFinishReason()).toUpperCase(Locale.ROOT);
		return SAFETY_FINISH_REASONS.stream().anyMatch(reason::contains);
	}

	private static AiException blockedError(Throwable cause) {
		return new AiException(AiErrorCode.BLOCKED, AiException.describe(AiErrorCode.BLOCKED), cause);
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
