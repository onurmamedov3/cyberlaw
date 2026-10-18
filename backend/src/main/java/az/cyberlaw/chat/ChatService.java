package az.cyberlaw.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.document.LanguageDetector;
import az.cyberlaw.rag.RetrievalService;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
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

	public record ChatRequest(String question, String conversationId, SearchScope scope, String language) {
	}

	public Flux<ServerSentEvent<Object>> stream(UUID userId, ChatRequest request) {
		if (!props.aiConfigured()) {
			return Flux.just(error(AiErrorCode.NOT_CONFIGURED));
		}
		String question = request.question().strip();
		String memoryKey = userId + ":" + request.conversationId();
		String answerLanguage = LanguageDetector.detect(question).equals("az") ? "az"
				: ("az".equals(request.language()) && !looksEnglish(question) ? "az" : "en");

		return Flux.defer(() -> {
			List<Source> sources = retrieval.search(question, userId,
					request.scope() != null ? request.scope() : SearchScope.BOTH);
			List<Message> history = chatMemory.get(memoryKey);
			ServerSentEvent<Object> sourcesEvent = event("sources", sources);

			if (sources.isEmpty() && history.isEmpty()) {
				String answer = Prompts.noSourcesAnswer(answerLanguage);
				return Flux.just(sourcesEvent, event("token", Map.of("text", answer)), event("done", Map.of()));
			}

			List<Message> messages = new ArrayList<>();
			messages.add(new SystemMessage(Prompts.chatSystem(sources, request.language())));
			messages.addAll(history);
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
		}).subscribeOn(Schedulers.boundedElastic()).onErrorResume(ex -> {
			AiErrorCode code = AiErrorCode.classify(ex);
			log.warn("Chat failed ({}): {}", code, ex.getMessage());
			return Flux.just(error(code));
		});
	}

	public void clear(UUID userId, String conversationId) {
		chatMemory.clear(userId + ":" + conversationId);
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

	private static ServerSentEvent<Object> error(AiErrorCode code) {
		return event("error", Map.of("code", code.name(), "message", AiException.describe(code)));
	}

}
