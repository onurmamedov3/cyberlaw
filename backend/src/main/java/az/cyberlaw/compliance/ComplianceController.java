package az.cyberlaw.compliance;

import java.util.List;
import java.util.UUID;

import az.cyberlaw.compliance.ComplianceDtos.LawDto;
import az.cyberlaw.compliance.ComplianceDtos.ReportDetailDto;
import az.cyberlaw.compliance.ComplianceDtos.ReportDto;
import az.cyberlaw.compliance.ComplianceDtos.RequirementDto;
import az.cyberlaw.compliance.ComplianceDtos.StartCheckRequest;
import az.cyberlaw.document.DocumentDto;
import az.cyberlaw.security.CurrentUser;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComplianceController {

	private final ComplianceService service;

	public ComplianceController(ComplianceService service) {
		this.service = service;
	}

	@GetMapping("/api/compliance/laws")
	public List<LawDto> laws() {
		return service.laws();
	}

	@GetMapping("/api/compliance/laws/{id}/requirements")
	public List<RequirementDto> requirements(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return service.requirements(CurrentUser.of(jwt), id);
	}

	@PostMapping("/api/compliance/reports")
	public ReportDto start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartCheckRequest request) {
		return service.start(CurrentUser.of(jwt), request.policyDocumentId(), request.lawDocumentId(),
				request.language());
	}

	@GetMapping("/api/compliance/reports")
	public List<ReportDto> list(@AuthenticationPrincipal Jwt jwt) {
		return service.list(CurrentUser.of(jwt));
	}

	@GetMapping("/api/compliance/reports/{id}")
	public ReportDetailDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return service.get(CurrentUser.of(jwt), id);
	}

	@DeleteMapping("/api/compliance/reports/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		service.delete(CurrentUser.of(jwt), id);
		return ResponseEntity.noContent().build();
	}

	/** Admin: generate the requirements checklist of an official law. */
	@PostMapping("/api/admin/documents/{id}/requirements")
	public DocumentDto extract(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return service.startExtraction(CurrentUser.of(jwt), id);
	}

}
