package az.cyberlaw.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cyberlaw")
public record CyberLawProperties(
		String geminiApiKey,
		Jwt jwt,
		Admin admin,
		String docsFolder,
		Uploads uploads,
		Rag rag,
		Compliance compliance) {

	public record Jwt(String secret, Duration expiration) {
	}

	public record Admin(String email, String password) {
	}

	public record Uploads(int maxPrivateDocuments, Duration privateTtl) {
	}

	public record Rag(int topK, double similarityThreshold, int chunkMaxChars, int historyMessages) {
	}

	public record Compliance(int maxRequirements, int maxExtractionCalls, int judgeBatchSize) {
	}

	/** True when a real Gemini key was put into .env (not the placeholder). */
	public boolean aiConfigured() {
		return geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.startsWith("PASTE_");
	}

}
