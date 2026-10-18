package az.cyberlaw.ai;

public class AiException extends RuntimeException {

	private final AiErrorCode code;

	public AiException(AiErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public AiException(AiErrorCode code, String message) {
		this(code, message, null);
	}

	public AiErrorCode getCode() {
		return code;
	}

	public static AiException from(Throwable error) {
		AiErrorCode code = AiErrorCode.classify(error);
		return new AiException(code, describe(code), error);
	}

	public static String describe(AiErrorCode code) {
		return switch (code) {
			case RATE_LIMIT -> "Gemini free-tier rate limit reached. Please wait a minute and try again.";
			case INVALID_KEY -> "The Gemini API key was rejected. Check GEMINI_API_KEY in the .env file.";
			case NOT_CONFIGURED -> "No Gemini API key configured. Put your key into GEMINI_API_KEY in the .env file.";
			case AI_ERROR -> "The AI service returned an error. Please try again.";
		};
	}

}
