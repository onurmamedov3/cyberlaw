package az.cyberlaw.chat;

import java.util.List;
import java.util.NoSuchElementException;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafetyBlockTest {

	private static ChatResponse chunk(String text, String finishReason) {
		ChatGenerationMetadata metadata = ChatGenerationMetadata.builder().finishReason(finishReason).build();
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text), metadata)));
	}

	private static void assertBlocked(Flux<ChatResponse> responses) {
		assertThatThrownBy(() -> ChatService.guardSafetyBlocks(responses).collectList().block())
			.isInstanceOfSatisfying(AiException.class, ex -> assertThat(ex.getCode()).isEqualTo(AiErrorCode.BLOCKED));
	}

	@Test
	void aNormalAnswerPassesThrough() {
		List<String> parts = ChatService.guardSafetyBlocks(Flux.just(chunk("Article 32 ", ""), chunk("requires [1].", "STOP")))
			.collectList()
			.block();

		assertThat(parts).containsExactly("Article 32 ", "requires [1].");
	}

	@Test
	void aSafetyFinishReasonIsBlocked() {
		assertBlocked(Flux.just(chunk("Here is", ""), chunk("", "SAFETY")));
		assertBlocked(Flux.just(chunk("", "PROHIBITED_CONTENT")));
	}

	@Test
	void aChunkWithoutContentIsBlocked() {
		// what Spring AI 2.0.1 throws when Gemini returns a blocked candidate without content
		assertBlocked(Flux.concat(Flux.just(chunk("Here is", "")), Flux.error(new NoSuchElementException("No value present"))));
	}

	@Test
	void anEmptyAnswerIsBlocked() {
		assertBlocked(Flux.empty());
		assertBlocked(Flux.just(chunk("", "STOP")));
	}

}
