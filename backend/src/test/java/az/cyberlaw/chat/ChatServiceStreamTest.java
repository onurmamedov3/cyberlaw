package az.cyberlaw.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import az.cyberlaw.chat.ChatService.ChatRequest;
import az.cyberlaw.chat.ConversationDtos.ConversationDto;
import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.rag.RetrievalService;
import az.cyberlaw.rag.SearchScope;
import az.cyberlaw.rag.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.codec.ServerSentEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The whole chat flow with a fake Gemini model, a fake search and a fake chat history. */
class ChatServiceStreamTest {

	private final ChatModel chatModel = mock(ChatModel.class);

	private final RetrievalService retrieval = mock(RetrievalService.class);

	private final ChatHistoryService history = mock(ChatHistoryService.class);

	private final CyberLawProperties props = new CyberLawProperties("test-key", null, null, null, null,
			new CyberLawProperties.Rag(6, 0.5, 2000, 10), new CyberLawProperties.Chat(5, 0), null);

	private final ChatService service = new ChatService(chatModel, history, retrieval, props);

	private final UUID userId = UUID.randomUUID();

	private final UUID chatId = UUID.randomUUID();

	private final Source source = new Source(1, UUID.randomUUID().toString(), "GDPR", "OFFICIAL", "Article 33",
			"Notification of a breach", "", "Notify within 72 hours.", 0.8);

	@BeforeEach
	void setUp() {
		when(retrieval.search(anyString(), any(), any())).thenReturn(List.of(source));
		when(history.saveExchange(any(), any(), anyString(), anyString(), any(), any()))
			.thenReturn(new ConversationDto(chatId.toString(), "When must a breach be reported?", "BOTH",
					Instant.now()));
	}

	@Test
	void aNewChatIsSavedOnceTheAnswerIsComplete() {
		answerWith("Within 72 hours ", "[1].");

		List<String> events = eventNames(ask(null, "When must a breach be reported?", false));

		assertThat(events).containsExactly("sources", "token", "token", "conversation", "done");
		verify(history).saveExchange(eq(userId), isNull(), eq("When must a breach be reported?"),
				eq("Within 72 hours [1]."), eq(List.of(source)), eq(SearchScope.BOTH));
	}

	@Test
	void aStoppedAnswerIsNotSaved() {
		answerWith("Within 72 hours ", "[1].");

		ask(null, "When must a breach be reported?", false).take(2).blockLast(); // the browser stops after a token

		verify(history, never()).saveExchange(any(), any(), anyString(), anyString(), any(), any());
	}

	@Test
	void regenerateFirstForgetsThePreviousAnswer() {
		answerWith("Second answer [1].");

		ask(chatId, "When must a breach be reported?", true).blockLast();

		InOrder order = inOrder(history);
		order.verify(history).forgetLastExchange(chatId, "When must a breach be reported?");
		order.verify(history).recentMessages(eq(chatId), anyInt());
		order.verify(history).saveExchange(eq(userId), eq(chatId), anyString(), eq("Second answer [1]."), any(), any());
	}

	@Test
	void aShortFollowUpIsSearchedWithThePreviousQuestion() {
		when(history.recentMessages(eq(chatId), anyInt())).thenReturn(List.of(
				new UserMessage("When must a breach be reported?"), new AssistantMessage("Within 72 hours [1].")));
		answerWith("Fines [1].");

		ask(chatId, "And the penalties?", false).blockLast();

		verify(retrieval).search(eq("When must a breach be reported?\nAnd the penalties?"), eq(userId),
				eq(SearchScope.BOTH));
	}

	@Test
	void someoneElsesChatIsRejectedBeforeAnythingIsStreamed() {
		when(history.owned(userId, chatId)).thenThrow(new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));

		assertThatThrownBy(() -> ask(chatId, "When must a breach be reported?", false))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
	}

	@Test
	void aDatabaseErrorIsNotReportedAsAnAiError() {
		when(retrieval.search(anyString(), any(), any()))
			.thenThrow(new DataAccessResourceFailureException("Connection refused"));

		ServerSentEvent<Object> last = ask(null, "When must a breach be reported?", false).blockLast();

		assertThat(last.event()).isEqualTo("error");
		assertThat(last.data().toString()).contains("INTERNAL");
	}

	private Flux<ServerSentEvent<Object>> ask(UUID conversationId, String question, boolean regenerate) {
		return service.stream(userId, new ChatRequest(question, conversationId, SearchScope.BOTH, "en", regenerate));
	}

	private void answerWith(String... chunks) {
		when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.fromArray(chunks)
			.map(text -> new ChatResponse(List.of(new Generation(new AssistantMessage(text))))));
	}

	private static List<String> eventNames(Flux<ServerSentEvent<Object>> events) {
		return events.map(ServerSentEvent::event).collectList().block();
	}

}
