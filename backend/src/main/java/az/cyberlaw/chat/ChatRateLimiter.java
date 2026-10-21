package az.cyberlaw.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.security.CurrentUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limits how many questions one user may ask per minute, so that a single user cannot use up the Gemini
 * quota that everyone shares. Admins are never limited and 0 turns the limit off. The admin can change
 * the limit on the Admin page; after a restart the configured value applies again.
 */
@Component
public class ChatRateLimiter {

	private static final long WINDOW_MS = 60_000;

	private final AtomicInteger perMinute;

	/** Times of each user's questions in the last minute, oldest first. */
	private final Map<UUID, Deque<Long>> recent = new ConcurrentHashMap<>();

	private final LongSupplier clock;

	@Autowired
	public ChatRateLimiter(CyberLawProperties props) {
		this(props.chat().maxQuestionsPerMinute(), System::currentTimeMillis);
	}

	ChatRateLimiter(int perMinute, LongSupplier clock) {
		this.perMinute = new AtomicInteger(perMinute);
		this.clock = clock;
	}

	public int getPerMinute() {
		return perMinute.get();
	}

	public void setPerMinute(int perMinute) {
		this.perMinute.set(perMinute);
	}

	/** Counts a question, or throws TOO_MANY_QUESTIONS with the seconds the user has to wait. */
	public void check(CurrentUser user) {
		int limit = perMinute.get();
		if (user.admin() || limit <= 0) {
			return;
		}
		long now = clock.getAsLong();
		// compute() runs atomically per user, so two tabs asking at the same moment are both counted
		recent.compute(user.id(), (id, times) -> {
			Deque<Long> lastMinute = times != null ? times : new ArrayDeque<>();
			while (!lastMinute.isEmpty() && lastMinute.peekFirst() <= now - WINDOW_MS) {
				lastMinute.pollFirst();
			}
			if (lastMinute.size() >= limit) {
				long waitMs = lastMinute.peekFirst() + WINDOW_MS - now;
				throw new ApiException(ErrorCode.TOO_MANY_QUESTIONS,
						Map.of("seconds", Math.max(1, TimeUnit.MILLISECONDS.toSeconds(waitMs + 999))));
			}
			lastMinute.addLast(now);
			return lastMinute;
		});
	}

	/** Forgets users who have not asked anything in the last minute. */
	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
	void forgetIdleUsers() {
		long cutoff = clock.getAsLong() - WINDOW_MS;
		for (UUID id : recent.keySet()) {
			recent.computeIfPresent(id, (key, times) -> times.isEmpty() || times.peekLast() <= cutoff ? null : times);
		}
	}

}
