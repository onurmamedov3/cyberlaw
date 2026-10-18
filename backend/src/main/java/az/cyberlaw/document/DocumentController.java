package az.cyberlaw.document;

import java.util.List;
import java.util.UUID;

import az.cyberlaw.security.CurrentUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentService service;

	public DocumentController(DocumentService service) {
		this.service = service;
	}

	@GetMapping
	public List<DocumentDto> list(@AuthenticationPrincipal Jwt jwt) {
		return service.listVisible(CurrentUser.of(jwt));
	}

	/** scope=PRIVATE for "My Documents", scope=OFFICIAL (admin only) for the Official Library. */
	@PostMapping(consumes = "multipart/form-data")
	public DocumentDto upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "PRIVATE") DocumentScope scope,
			@RequestParam(required = false) String title) {
		return service.upload(CurrentUser.of(jwt), file, scope, title);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		service.delete(CurrentUser.of(jwt), id);
		return ResponseEntity.noContent().build();
	}

}
