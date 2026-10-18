package az.cyberlaw.ai;

import java.util.List;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/** Calls Gemini and parses the answer as JSON into Java records. */
@Component
public class StructuredLlm {

	private final ChatModel chatModel;

	public StructuredLlm(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	public <T> T call(String label, String instructions, BeanOutputConverter<T> converter) {
		// plain concatenation (no template rendering): legal texts may contain { and }
		Prompt prompt = new Prompt(List.of(new UserMessage(instructions + "\n\n" + converter.getFormat())));
		ChatResponse response = AiRetry.call(label, () -> chatModel.call(prompt));
		String text = response.getResult() != null ? response.getResult().getOutput().getText() : null;
		if (text == null || text.isBlank()) {
			throw new AiException(AiErrorCode.AI_ERROR, "The AI returned an empty answer.");
		}
		return converter.convert(text);
	}

}
