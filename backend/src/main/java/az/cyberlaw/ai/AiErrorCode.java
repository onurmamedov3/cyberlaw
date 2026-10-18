package az.cyberlaw.ai;

import java.util.Locale;

public enum AiErrorCode {

	RATE_LIMIT, INVALID_KEY, NOT_CONFIGURED, AI_ERROR;

	/** Inspects the whole cause chain of an exception thrown by the Gemini client. */
	public static AiErrorCode classify(Throwable error) {
		if (error instanceof AiException aiException) {
			return aiException.getCode();
		}
		StringBuilder text = new StringBuilder();
		for (Throwable t = error; t != null; t = t.getCause()) {
			text.append(t.getClass().getSimpleName()).append(' ').append(t.getMessage()).append(' ');
			if (t.getCause() == t) {
				break;
			}
		}
		String all = text.toString().toLowerCase(Locale.ROOT);
		if (all.contains("429") || all.contains("resource_exhausted") || all.contains("quota")
				|| all.contains("rate limit")) {
			return RATE_LIMIT;
		}
		if (all.contains("api key not valid") || all.contains("api_key_invalid") || all.contains("permission_denied")
				|| all.contains("unauthenticated")) {
			return INVALID_KEY;
		}
		return AI_ERROR;
	}

}
