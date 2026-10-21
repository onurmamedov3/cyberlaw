package az.cyberlaw.chat;

import az.cyberlaw.chat.ChatService.ChatRequest;
import az.cyberlaw.security.CurrentUser;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Object>> stream(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChatRequest request) {
		return chatService.stream(CurrentUser.of(jwt).id(), request);
	}

	/** "New chat": forget the conversation history. */
	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt, @PathVariable String conversationId) {
		chatService.clear(CurrentUser.of(jwt).id(), conversationId);
		return ResponseEntity.noContent().build();
	}

}
