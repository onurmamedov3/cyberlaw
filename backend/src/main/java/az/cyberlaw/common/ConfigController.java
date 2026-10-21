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

	public record AppConfig(int maxPrivateDocuments, long privateTtlDays, long maxFileSizeMb, int maxQuestionLength) {
	}

	private final AppConfig config;

	public ConfigController(CyberLawProperties props,
			@Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
		this.config = new AppConfig(props.uploads().maxPrivateDocuments(), props.uploads().privateTtl().toDays(),
				maxFileSize.toMegabytes(), ChatService.MAX_QUESTION);
	}

	@GetMapping("/api/config")
	public AppConfig config() {
		return config;
	}

}
