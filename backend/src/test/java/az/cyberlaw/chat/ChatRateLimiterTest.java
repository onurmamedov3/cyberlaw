package az.cyberlaw.chat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.security.CurrentUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRateLimiterTest {

	private final AtomicLong now = new AtomicLong(1_000_000);

	private final ChatRateLimiter limiter = new ChatRateLimiter(3, now::get);

	private final CurrentUser user = new CurrentUser(UUID.randomUUID(), false);

	@Test
	void allowsTheLimitThenAsksTheUserToWait() {
		askTimes(user, 3);
		now.addAndGet(20_000);

		assertThatThrownBy(() -> limiter.check(user)).isInstanceOfSatisfying(ApiException.class, ex -> {
			org.assertj.core.api.Assertions.assertThat(ex.getCode()).isEqualTo(ErrorCode.TOO_MANY_QUESTIONS);
			// the first question leaves the one-minute window 40 seconds from now
			org.assertj.core.api.Assertions.assertThat(ex.getParams()).containsEntry("seconds", 40L);
		});
	}

	@Test
	void allowsAgainOnceTheMinuteHasPassed() {
		askTimes(user, 3);
		now.addAndGet(60_000);

		assertThatCode(() -> limiter.check(user)).doesNotThrowAnyException();
	}

	@Test
	void countsEachUserSeparately() {
		askTimes(user, 3);

		assertThatCode(() -> limiter.check(new CurrentUser(UUID.randomUUID(), false))).doesNotThrowAnyException();
	}

	@Test
	void neverLimitsAdmins() {
		CurrentUser admin = new CurrentUser(UUID.randomUUID(), true);

		assertThatCode(() -> askTimes(admin, 10)).doesNotThrowAnyException();
	}

	@Test
	void zeroTurnsTheLimitOffAndChangesApplyAtOnce() {
		askTimes(user, 3);
		limiter.setPerMinute(0);
		assertThatCode(() -> askTimes(user, 10)).doesNotThrowAnyException();

		// questions asked while the limit was off are not counted
		limiter.setPerMinute(5);
		askTimes(user, 2);
		assertThatThrownBy(() -> limiter.check(user)).isInstanceOf(ApiException.class);
	}

	private void askTimes(CurrentUser who, int times) {
		for (int i = 0; i < times; i++) {
			limiter.check(who);
		}
	}

}
