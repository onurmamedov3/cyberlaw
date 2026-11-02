package az.cyberlaw.ai;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting.HarmBlockThreshold;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting.HarmCategory;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Spring ignores misspelled properties silently, so this checks that application.yml really sets the filter. */
class SafetySettingsConfigTest {

	@Test
	void applicationYmlSetsGooglesSafetyFilter() throws Exception {
		List<PropertySource<?>> yml = new YamlPropertySourceLoader().load("application",
				new ClassPathResource("application.yml"));
		GoogleGenAiChatProperties chat = new Binder(ConfigurationPropertySources.from(yml))
			.bind(GoogleGenAiChatProperties.CONFIG_PREFIX, GoogleGenAiChatProperties.class)
			.get();

		assertThat(chat.getSafetySettings())
			.extracting(GoogleGenAiSafetySetting::getCategory, GoogleGenAiSafetySetting::getThreshold)
			.containsExactlyInAnyOrder(tuple(HarmCategory.HARM_CATEGORY_HARASSMENT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
					tuple(HarmCategory.HARM_CATEGORY_HATE_SPEECH, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
					tuple(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
					tuple(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, HarmBlockThreshold.BLOCK_ONLY_HIGH));
	}

}
