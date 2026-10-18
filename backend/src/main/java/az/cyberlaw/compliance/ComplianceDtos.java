package az.cyberlaw.compliance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public final class ComplianceDtos {

	private ComplianceDtos() {
	}

	public record StartCheckRequest(@NotNull UUID policyDocumentId, @NotNull UUID lawDocumentId, String language) {
	}

	public record LawDto(String id, String title, String language, long requirementCount) {
	}

	public record RequirementDto(int position, String article, String title, String description) {
		static RequirementDto of(Requirement r) {
			return new RequirementDto(r.getPosition(), r.getArticle(), r.getTitle(), r.getDescription());
		}
	}

	public record ReportDto(String id, String policyDocumentId, String policyTitle, String lawDocumentId,
			String lawTitle, String status, String language, int total, int processed, int met, int partial,
			int missing, Integer score, String errorMessage, Instant createdAt, Instant finishedAt) {
		static ReportDto of(ComplianceReport r, String policyTitle, String lawTitle) {
			return new ReportDto(r.getId().toString(), r.getPolicyDocumentId().toString(), policyTitle,
					r.getLawDocumentId().toString(), lawTitle, r.getStatus().name(), r.getLanguage(), r.getTotal(),
					r.getProcessed(), r.getMet(), r.getPartial(), r.getMissing(), r.getScore(), r.getErrorMessage(),
					r.getCreatedAt(), r.getFinishedAt());
		}
	}

	public record ResultDto(int position, String article, String title, String description, String status,
			String explanation, String evidence) {
		static ResultDto of(ComplianceResult r) {
			return new ResultDto(r.getPosition(), r.getArticle(), r.getTitle(), r.getDescription(),
					r.getStatus().name(), r.getExplanation(), r.getEvidence());
		}
	}

	public record ReportDetailDto(ReportDto report, List<ResultDto> results) {
	}

}
