package az.cyberlaw.compliance;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One obligation extracted from an official law, e.g. "Appoint a data protection officer (Article 37)". */
@Entity
@Table(name = "requirement")
public class Requirement {

	@Id
	private UUID id;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;

	@Column(nullable = false)
	private int position;

	private String article;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	protected Requirement() {
	}

	public Requirement(UUID documentId, int position, String article, String title, String description) {
		this.id = UUID.randomUUID();
		this.documentId = documentId;
		this.position = position;
		this.article = article;
		this.title = title;
		this.description = description;
	}

	public UUID getId() {
		return id;
	}

	public UUID getDocumentId() {
		return documentId;
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

}
