package az.cyberlaw.ai;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retries Gemini calls that hit the free-tier rate limit, with a growing pause. */
public final class AiRetry {

	private static final Logger log = LoggerFactory.getLogger(AiRetry.class);

	private static final int MAX_ATTEMPTS = 4;

	private static final long BASE_WAIT_MS = 15_000;

	private AiRetry() {
	}

	public static <T> T call(String label, Supplier<T> action) {
		for (int attempt = 1;; attempt++) {
			try {
				return action.get();
			}
			catch (RuntimeException ex) {
				AiErrorCode code = AiErrorCode.classify(ex);
				if (code != AiErrorCode.RATE_LIMIT || attempt >= MAX_ATTEMPTS) {
					throw AiException.from(ex);
				}
				long wait = BASE_WAIT_MS * attempt;
				log.warn("{}: Gemini rate limit hit, retrying in {}s (attempt {}/{})", label, wait / 1000, attempt,
						MAX_ATTEMPTS);
				sleep(wait);
			}
		}
	}

	public static void run(String label, Runnable action) {
		call(label, () -> {
			action.run();
			return null;
		});
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for rate limit", ex);
		}
	}

}
