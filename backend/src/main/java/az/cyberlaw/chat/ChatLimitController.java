package az.cyberlaw.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Admin: read and change the per-user chat limit while the app is running. */
@RestController
public class ChatLimitController {

	private static final Logger log = LoggerFactory.getLogger(ChatLimitController.class);

	private final ChatRateLimiter limiter;

	public ChatLimitController(ChatRateLimiter limiter) {
		this.limiter = limiter;
	}

	/** Questions per user per minute; 0 = no limit. */
	public record ChatLimit(@Min(0) @Max(1000) int perMinute) {
	}

	@GetMapping("/api/admin/chat-limit")
	public ChatLimit get() {
		return new ChatLimit(limiter.getPerMinute());
	}

	@PutMapping("/api/admin/chat-limit")
	public ChatLimit set(@Valid @RequestBody ChatLimit limit) {
		limiter.setPerMinute(limit.perMinute());
		log.info("Chat limit set to {} question(s) per user per minute", limit.perMinute());
		return limit;
	}

}
