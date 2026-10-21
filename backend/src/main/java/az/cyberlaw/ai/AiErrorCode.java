package az.cyberlaw.ai;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;

import org.springframework.dao.DataAccessException;

public enum AiErrorCode {

	RATE_LIMIT, INVALID_KEY, NOT_CONFIGURED, AI_ERROR;

	/** Inspects the whole cause chain of an exception thrown while calling Gemini. */
	public static AiErrorCode classify(Throwable error) {
		for (Throwable t : causes(error)) {
			if (t instanceof AiException aiException) {
				return aiException.getCode();
			}
			if (t instanceof ApiException api) {
				// the HTTP status from the Gemini SDK is more reliable than the error text
				if (api.code() == 429) {
					return RATE_LIMIT;
				}
				if (api.code() == 401 || api.code() == 403 || mentionsApiKey(api.message())) {
					return INVALID_KEY;
				}
				return AI_ERROR;
			}
		}
		String all = text(error);
		if (all.contains("429") || all.contains("resource_exhausted") || all.contains("quota")
				|| all.contains("rate limit")) {
			return RATE_LIMIT;
		}
		if (mentionsApiKey(all) || all.contains("permission_denied") || all.contains("unauthenticated")) {
			return INVALID_KEY;
		}
		return AI_ERROR;
	}

	/**
	 * True when the error comes from Gemini (or Spring AI around it). Errors of our database or our own code
	 * are not, and must not be reported as "the AI service returned an error".
	 */
	public static boolean isAiError(Throwable error) {
		List<Throwable> chain = causes(error);
		if (chain.stream().anyMatch(t -> t instanceof DataAccessException || t instanceof SQLException)) {
			return false;
		}
		for (Throwable t : chain) {
			if (t instanceof AiException || t instanceof ApiException || t instanceof GenAiIOException
					|| t.getClass().getName().startsWith("org.springframework.ai.")) {
				return true;
			}
		}
		String all = text(error);
		return all.contains("resource_exhausted") || all.contains("quota") || all.contains("rate limit")
				|| mentionsApiKey(all);
	}

	private static boolean mentionsApiKey(String message) {
		String text = message != null ? message.toLowerCase(Locale.ROOT) : "";
		return text.contains("api key not valid") || text.contains("api_key_invalid");
	}

	private static List<Throwable> causes(Throwable error) {
		List<Throwable> chain = new ArrayList<>();
		for (Throwable t = error; t != null && !chain.contains(t); t = t.getCause()) {
			chain.add(t);
		}
		return chain;
	}

	private static String text(Throwable error) {
		StringBuilder text = new StringBuilder();
		for (Throwable t : causes(error)) {
			text.append(t.getClass().getSimpleName()).append(' ').append(t.getMessage()).append(' ');
		}
		return text.toString().toLowerCase(Locale.ROOT);
	}

}
