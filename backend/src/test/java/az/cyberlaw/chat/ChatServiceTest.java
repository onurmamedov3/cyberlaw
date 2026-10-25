package az.cyberlaw.chat;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

	private final List<Message> history = List.of(new UserMessage("What must a data operator do after a breach?"),
			new AssistantMessage("Notify the authority within 72 hours [1][2]. Inform the people concerned [3]."));

	@Test
	void shortFollowUpsAreSearchedWithThePreviousQuestion() {
		assertThat(ChatService.searchQuery("And the penalties?", history))
			.isEqualTo("What must a data operator do after a breach?\nAnd the penalties?");
	}

	@Test
	void longerQuestionsAreSearchedAsTheyAre() {
		String question = "Which acts count as crimes against computer information in Azerbaijan?";
		assertThat(ChatService.searchQuery(question, history)).isEqualTo(question);
	}

	@Test
	void theFirstQuestionIsSearchedAsItIs() {
		assertThat(ChatService.searchQuery("What is GDPR?", List.of())).isEqualTo("What is GDPR?");
	}

	@Test
	void citationMarkersAreRemovedFromEarlierAnswers() {
		List<Message> cleaned = ChatService.withoutCitations(history);

		assertThat(cleaned.get(0).getText()).isEqualTo("What must a data operator do after a breach?");
		assertThat(cleaned.get(1)).isInstanceOf(AssistantMessage.class);
		assertThat(cleaned.get(1).getText())
			.isEqualTo("Notify the authority within 72 hours. Inform the people concerned.");
	}

	@Test
	void markdownLinksAreKept() {
		List<Message> cleaned = ChatService
			.withoutCitations(List.of(new AssistantMessage("See [1](https://e-qanun.az) for the full text.")));
		assertThat(cleaned.get(0).getText()).isEqualTo("See [1](https://e-qanun.az) for the full text.");
	}

}
