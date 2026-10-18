package az.cyberlaw.common;

import java.util.Map;

import az.cyberlaw.config.CyberLawProperties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private final CyberLawProperties props;

	public HealthController(CyberLawProperties props) {
		this.props = props;
	}

	@GetMapping("/api/health")
	public Map<String, Object> health() {
		return Map.of("status", "UP", "aiConfigured", props.aiConfigured());
	}

}
