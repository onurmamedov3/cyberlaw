package az.cyberlaw.compliance;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The verdict for one requirement in a compliance report. */
@Entity
@Table(name = "compliance_result")
public class ComplianceResult {

	public enum Verdict {

		MET, PARTIAL, MISSING

	}

	@Id
	private UUID id;

	@Column(name = "report_id", nullable = false)
	private UUID reportId;

	@Column(nullable = false)
	private int position;

	private String article;

	@Column(nullable = false)
	private String title;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Verdict status;

	private String explanation;

	private String evidence;

	protected ComplianceResult() {
	}

	public ComplianceResult(UUID reportId, Requirement requirement, Verdict status, String explanation,
			String evidence) {
		this.id = UUID.randomUUID();
		this.reportId = reportId;
		this.position = requirement.getPosition();
		this.article = requirement.getArticle();
		this.title = requirement.getTitle();
		this.description = requirement.getDescription();
		this.status = status;
		this.explanation = explanation;
		this.evidence = evidence;
	}

	public UUID getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public String getArticle() {
		return article;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Verdict getStatus() {
		return status;
	}

	public String getExplanation() {
		return explanation;
	}

	public String getEvidence() {
		return evidence;
	}

}
