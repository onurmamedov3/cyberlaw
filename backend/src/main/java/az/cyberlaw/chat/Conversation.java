package az.cyberlaw.chat;

import java.time.Instant;
import java.util.UUID;

import az.cyberlaw.rag.SearchScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A saved chat, shown in the sidebar. Its messages are {@link ConversationMessage}s. */
@Entity
@Table(name = "chat_conversation")
public class Conversation {

	@Id
	private UUID id;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(nullable = false)
	private String title;

	/** The search scope used last, restored when the chat is opened again. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SearchScope scope;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Conversation() {
	}

	public Conversation(UUID ownerId, String title, SearchScope scope) {
		this.id = UUID.randomUUID();
		this.ownerId = ownerId;
		this.title = title;
		this.scope = scope;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	/** A new answer was added: the chat moves to the top of the list. */
	public void touch(SearchScope scope) {
		this.scope = scope;
		this.updatedAt = Instant.now();
	}

	public void rename(String title) {
		this.title = title;
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getTitle() {
		return title;
	}

	public SearchScope getScope() {
		return scope;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
