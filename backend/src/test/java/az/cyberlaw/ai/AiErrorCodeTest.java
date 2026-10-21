package az.cyberlaw.ai;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiErrorCodeTest {

	@Test
	void usesTheHttpStatusOfTheGeminiSdk() {
		var quota = new RuntimeException("Failed to generate content",
				new ClientException(429, "RESOURCE_EXHAUSTED", "Quota exceeded"));
		var badKey = new ClientException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.");
		var forbidden = new ClientException(403, "PERMISSION_DENIED", "Permission denied");
		var outage = new ServerException(503, "UNAVAILABLE", "The model is overloaded");

		assertThat(AiErrorCode.classify(quota)).isEqualTo(AiErrorCode.RATE_LIMIT);
		assertThat(AiErrorCode.classify(badKey)).isEqualTo(AiErrorCode.INVALID_KEY);
		assertThat(AiErrorCode.classify(forbidden)).isEqualTo(AiErrorCode.INVALID_KEY);
		assertThat(AiErrorCode.classify(outage)).isEqualTo(AiErrorCode.AI_ERROR);
		assertThat(AiErrorCode.isAiError(quota)).isTrue();
		assertThat(AiErrorCode.isAiError(outage)).isTrue();
	}

	@Test
	void databaseAndProgrammingErrorsAreNotAiErrors() {
		assertThat(AiErrorCode.isAiError(new DataAccessResourceFailureException("Connection refused"))).isFalse();
		// even when the text happens to look like a Gemini error
		assertThat(AiErrorCode.isAiError(new DataAccessResourceFailureException("quota of connections reached")))
			.isFalse();
		assertThat(AiErrorCode.isAiError(new NullPointerException())).isFalse();
	}

	@Test
	void fallsBackToTheErrorText() {
		var wrapped = new IllegalStateException("429 RESOURCE_EXHAUSTED: rate limit");
		assertThat(AiErrorCode.isAiError(wrapped)).isTrue();
		assertThat(AiErrorCode.classify(wrapped)).isEqualTo(AiErrorCode.RATE_LIMIT);
	}

	@Test
	void retryPassesNonAiErrorsThroughUnchanged() {
		assertThatThrownBy(() -> AiRetry.call("search", () -> {
			throw new DataAccessResourceFailureException("Connection refused");
		})).isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void retryReportsOtherAiErrorsWithTheirCode() {
		assertThatThrownBy(() -> AiRetry.call("search", () -> {
			throw new ClientException(400, "INVALID_ARGUMENT", "API key not valid.");
		})).isInstanceOfSatisfying(AiException.class,
				ex -> assertThat(ex.getCode()).isEqualTo(AiErrorCode.INVALID_KEY));
	}

}
