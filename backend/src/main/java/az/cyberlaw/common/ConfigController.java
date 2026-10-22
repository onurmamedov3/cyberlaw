package az.cyberlaw.common;

import az.cyberlaw.chat.ChatService;
import az.cyberlaw.config.CyberLawProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The limits the UI shows to users, so the frontend never keeps its own copy of application.yml values. */
@RestController
public class ConfigController {

	/** historyDays: chats are deleted after this many days without activity, 0 = kept until deleted. */
	public record AppConfig(int maxPrivateDocuments, long privateTtlDays, long maxFileSizeMb, int maxQuestionLength,
			int historyDays) {
	}

	private final AppConfig config;

	public ConfigController(CyberLawProperties props,
			@Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
		this.config = new AppConfig(props.uploads().maxPrivateDocuments(), props.uploads().privateTtl().toDays(),
				maxFileSize.toMegabytes(), ChatService.MAX_QUESTION, props.chat().historyDays());
	}

	@GetMapping("/api/config")
	public AppConfig config() {
		return config;
	}

}
