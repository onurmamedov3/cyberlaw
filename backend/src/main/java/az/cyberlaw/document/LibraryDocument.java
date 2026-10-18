package az.cyberlaw.document;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An uploaded file. Its text chunks and embeddings live in the vector_store table. */
@Entity
@Table(name = "library_document")
public class LibraryDocument {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(name = "original_filename", nullable = false)
	private String originalFilename;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(nullable = false)
	private String sha256;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DocumentScope scope;

	@Column(name = "owner_id")
	private UUID ownerId;

	private String language;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DocumentStatus status;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "chunk_count", nullable = false)
	private int chunkCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "requirements_status", nullable = false)
	private RequirementsStatus requirementsStatus = RequirementsStatus.NONE;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	protected LibraryDocument() {
	}

	public LibraryDocument(String title, String originalFilename, String contentType, long sizeBytes, String sha256,
			DocumentScope scope, UUID ownerId, Instant expiresAt) {
		this.id = UUID.randomUUID();
		this.title = title;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.sha256 = sha256;
		this.scope = scope;
		this.ownerId = ownerId;
		this.status = DocumentStatus.PROCESSING;
		this.createdAt = Instant.now();
		this.expiresAt = expiresAt;
	}

	public void markReady(int chunkCount, String language) {
		this.status = DocumentStatus.READY;
		this.chunkCount = chunkCount;
		this.language = language;
		this.errorMessage = null;
	}

	public void markFailed(String errorMessage) {
		this.status = DocumentStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	public void markProcessing() {
		this.status = DocumentStatus.PROCESSING;
		this.errorMessage = null;
	}

	public boolean isVisibleTo(UUID userId) {
		return scope == DocumentScope.OFFICIAL || userId.equals(ownerId);
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getSha256() {
		return sha256;
	}

	public DocumentScope getScope() {
		return scope;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getLanguage() {
		return language;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getChunkCount() {
		return chunkCount;
	}

	public RequirementsStatus getRequirementsStatus() {
		return requirementsStatus;
	}

	public void setRequirementsStatus(RequirementsStatus requirementsStatus) {
		this.requirementsStatus = requirementsStatus;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

}
