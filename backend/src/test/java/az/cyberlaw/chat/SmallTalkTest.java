package az.cyberlaw.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SmallTalkTest {

	@ParameterizedTest
	@ValueSource(strings = { "Salam", "salam!", "SALAM", "Salam aleykum", "Sağ ol", "Çox sağ olun",
			"Təşəkkür edirəm", "TƏŞƏKKÜRLƏR", "Sabahınız xeyir", "Necəsən?", "Hələlik", "Hi", "Hello there",
			"Thanks!", "Thank you very much", "Good morning", "How are you?", "ok", "Bye, see you later" })
	void recognisesGreetingsThanksAndGoodbyes(String message) {
		assertThat(SmallTalk.matches(message)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Salam, GDPR nədir?", "Maddə 5 nə deyir?", "Thanks, and what about fines?",
			"Hello, what is GDPR?", "What is Article 32?", "ok 32", "Fərdi məlumat nədir", "GDPR" })
	void letsRealQuestionsThrough(String message) {
		assertThat(SmallTalk.matches(message)).isFalse();
	}

	@Test
	void rejectsEmptyAndLongMessages() {
		assertThat(SmallTalk.matches(null)).isFalse();
		assertThat(SmallTalk.matches("   ")).isFalse();
		assertThat(SmallTalk.matches("?!")).isFalse();
		assertThat(SmallTalk.matches("hi hi hi hi hi hi hi")).isFalse();
	}

}
