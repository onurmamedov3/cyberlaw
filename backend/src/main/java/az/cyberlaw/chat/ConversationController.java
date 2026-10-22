package az.cyberlaw.chat;

import java.util.List;
import java.util.UUID;

import az.cyberlaw.chat.ConversationDtos.ConversationDetailDto;
import az.cyberlaw.chat.ConversationDtos.ConversationDto;
import az.cyberlaw.chat.ConversationDtos.RenameRequest;
import az.cyberlaw.security.CurrentUser;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The chat history in the sidebar. Every user sees only their own chats, the admin included. */
@RestController
@RequestMapping("/api/chat/conversations")
public class ConversationController {

	private final ChatHistoryService history;

	public ConversationController(ChatHistoryService history) {
		this.history = history;
	}

	@GetMapping
	public List<ConversationDto> list(@AuthenticationPrincipal Jwt jwt) {
		return history.list(CurrentUser.of(jwt).id());
	}

	@GetMapping("/{id}")
	public ConversationDetailDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return history.get(CurrentUser.of(jwt).id(), id);
	}

	@PatchMapping("/{id}")
	public ConversationDto rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody RenameRequest request) {
		return history.rename(CurrentUser.of(jwt).id(), id, request.title());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		history.delete(CurrentUser.of(jwt).id(), id);
		return ResponseEntity.noContent().build();
	}

	/** Settings → "Delete all chats". */
	@DeleteMapping
	public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal Jwt jwt) {
		history.deleteAll(CurrentUser.of(jwt).id());
		return ResponseEntity.noContent().build();
	}

}
