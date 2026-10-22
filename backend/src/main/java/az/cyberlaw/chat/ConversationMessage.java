package az.cyberlaw.chat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A question or an answer of a saved chat, in the order given by position. */
@Entity
@Table(name = "chat_message")
public class ConversationMessage {

	public enum Role {

		USER, ASSISTANT

	}

	@Id
	private UUID id;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(nullable = false)
	private int position;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ConversationMessage() {
	}

	public ConversationMessage(UUID conversationId, int position, Role role, String content) {
		this.id = UUID.randomUUID();
		this.conversationId = conversationId;
		this.position = position;
		this.role = role;
		this.content = content;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public int getPosition() {
		return position;
	}

	public Role getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
