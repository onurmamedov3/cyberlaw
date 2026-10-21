package az.cyberlaw.chat;

import java.util.List;
import java.util.UUID;

import az.cyberlaw.chat.ChatService.ChatRequest;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.rag.RetrievalService;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.codec.ServerSentEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The whole chat flow with a fake Gemini model and a fake search. */
class ChatServiceStreamTest {

	private final ChatModel chatModel = mock(ChatModel.class);

	private final RetrievalService retrieval = mock(RetrievalService.class);

	private final ChatMemory memory = MessageWindowChatMemory.builder()
		.chatMemoryRepository(new InMemoryChatMemoryRepository())
		.maxMessages(10)
		.build();

	private final CyberLawProperties props = new CyberLawProperties("test-key", null, null, null, null,
			new CyberLawProperties.Rag(6, 0.5, 2000, 10), new CyberLawProperties.Chat(5), null);

	private final ChatService service = new ChatService(chatModel, memory, retrieval, props);

	private final UUID userId = UUID.randomUUID();

	private final Source source = new Source(1, UUID.randomUUID().toString(), "GDPR", "OFFICIAL", "Article 33",
			"Notification of a breach", "", "Notify within 72 hours.", 0.8);

	@Test
	void streamsSourcesTokensAndDoneThenRemembersTheExchange() {
		when(retrieval.search(anyString(), any(), any())).thenReturn(List.of(source));
		answerWith("Within 72 hours ", "[1].");

		List<String> events = eventNames(ask("When must a breach be reported?", false));

		assertThat(events).containsExactly("sources", "token", "token", "done");
		assertThat(memory.get(key())).extracting(Message::getText)
			.containsExactly("When must a breach be reported?", "Within 72 hours [1].");
	}

	@Test
	void regenerateReplacesThePreviousAnswerInsteadOfAddingToIt() {
		when(retrieval.search(anyString(), any(), any())).thenReturn(List.of(source));
		answerWith("First answer [1].");
		ask("When must a breach be reported?", false).blockLast();

		answerWith("Second answer [1].");
		ask("When must a breach be reported?", true).blockLast();

		assertThat(memory.get(key())).extracting(Message::getText)
			.containsExactly("When must a breach be reported?", "Second answer [1].");
		// the new answer was written without the old one in the prompt: system + question only
		ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(2)).stream(prompts.capture());
		assertThat(prompts.getValue().getInstructions()).hasSize(2);
	}

	@Test
	void aShortFollowUpIsSearchedWithThePreviousQuestion() {
		when(retrieval.search(anyString(), any(), any())).thenReturn(List.of(source));
		answerWith("Within 72 hours [1].");
		ask("When must a breach be reported?", false).blockLast();

		ask("And the penalties?", false).blockLast();

		verify(retrieval).search(eq("When must a breach be reported?\nAnd the penalties?"), eq(userId),
				eq(SearchScope.BOTH));
	}

	@Test
	void aDatabaseErrorIsNotReportedAsAnAiError() {
		when(retrieval.search(anyString(), any(), any()))
			.thenThrow(new DataAccessResourceFailureException("Connection refused"));

		ServerSentEvent<Object> last = ask("When must a breach be reported?", false).blockLast();

		assertThat(last.event()).isEqualTo("error");
		assertThat(last.data().toString()).contains("INTERNAL");
	}

	private Flux<ServerSentEvent<Object>> ask(String question, boolean regenerate) {
		return service.stream(userId, new ChatRequest(question, "c1", SearchScope.BOTH, "en", regenerate));
	}

	private void answerWith(String... chunks) {
		when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.fromArray(chunks)
			.map(text -> new ChatResponse(List.of(new Generation(new AssistantMessage(text))))));
	}

	private static List<String> eventNames(Flux<ServerSentEvent<Object>> events) {
		return events.map(ServerSentEvent::event).collectList().block();
	}

	private String key() {
		return userId + ":c1";
	}

}
