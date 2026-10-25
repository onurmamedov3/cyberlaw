package az.cyberlaw.compliance;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compliance_report")
public class ComplianceReport {

	public enum Status {

		RUNNING, DONE, FAILED

	}

	@Id
	private UUID id;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "policy_document_id", nullable = false)
	private UUID policyDocumentId;

	@Column(name = "law_document_id", nullable = false)
	private UUID lawDocumentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

	@Column(nullable = false)
	private String language;

	private int total;

	private int processed;

	private int met;

	private int partial;

	private int missing;

	private Integer score;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	protected ComplianceReport() {
	}

	public ComplianceReport(UUID ownerId, UUID policyDocumentId, UUID lawDocumentId, String language, int total) {
		this.id = UUID.randomUUID();
		this.ownerId = ownerId;
		this.policyDocumentId = policyDocumentId;
		this.lawDocumentId = lawDocumentId;
		this.language = language;
		this.total = total;
		this.status = Status.RUNNING;
		this.createdAt = Instant.now();
	}

	public void addResult(ComplianceResult.Verdict verdict) {
		processed++;
		switch (verdict) {
			case MET -> met++;
			case PARTIAL -> partial++;
			case MISSING -> missing++;
		}
	}

	public void finish() {
		this.status = Status.DONE;
		this.score = total == 0 ? 0 : (int) Math.round((met + 0.5 * partial) * 100.0 / total);
		this.finishedAt = Instant.now();
	}

	/** The code (ErrorCode or AiErrorCode) lets the UI translate the English message. */
	public void fail(String code, String message) {
		this.status = Status.FAILED;
		this.errorCode = code;
		this.errorMessage = message;
		this.finishedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public UUID getPolicyDocumentId() {
		return policyDocumentId;
	}

	public UUID getLawDocumentId() {
		return lawDocumentId;
	}

	public Status getStatus() {
		return status;
	}

	public String getLanguage() {
		return language;
	}

	public int getTotal() {
		return total;
	}

	public int getProcessed() {
		return processed;
	}

	public int getMet() {
		return met;
	}

	public int getPartial() {
		return partial;
	}

	public int getMissing() {
		return missing;
	}

	public Integer getScore() {
		return score;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

}
