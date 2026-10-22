package az.cyberlaw.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatHistoryServiceTest {

	@Test
	void aShortFirstQuestionIsTheTitle() {
		assertThat(ChatHistoryService.title("  What is   GDPR? ")).isEqualTo("What is GDPR?");
	}

	@Test
	void aLongFirstQuestionIsCutAtAWord() {
		String title = ChatHistoryService
			.title("What are the obligations of a personal data operator in Azerbaijan after a data breach?");
		assertThat(title).isEqualTo("What are the obligations of a personal data operator in…");
	}

	@Test
	void onlyCitedPassagesAreKept() {
		assertThat(Citations.cited("Notify within 72 hours [1][3]. See [the law](https://e-qanun.az)."))
			.containsExactlyInAnyOrder(1, 3);
	}

}
