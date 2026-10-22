package az.cyberlaw.chat;

import java.util.UUID;

import az.cyberlaw.rag.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A copy of a passage an answer cited as [position]. text is null once a private document is deleted. */
@Entity
@Table(name = "chat_message_source")
public class MessageSource {

	@Id
	private UUID id;

	@Column(name = "message_id", nullable = false)
	private UUID messageId;

	@Column(nullable = false)
	private int position;

	@Column(name = "document_id")
	private UUID documentId;

	@Column(name = "document_title", nullable = false)
	private String documentTitle;

	@Column(nullable = false)
	private String scope;

	private String article;

	@Column(name = "article_title")
	private String articleTitle;

	private String chapter;

	private String text;

	@Column(nullable = false)
	private double score;

	protected MessageSource() {
	}

	public MessageSource(UUID messageId, Source source) {
		this.id = UUID.randomUUID();
		this.messageId = messageId;
		this.position = source.index();
		this.documentId = source.documentId().isEmpty() ? null : UUID.fromString(source.documentId());
		this.documentTitle = cut(source.documentTitle(), 300);
		this.scope = source.scope();
		this.article = cut(source.article(), 100);
		this.articleTitle = cut(source.articleTitle(), 300);
		this.chapter = cut(source.chapter(), 300);
		this.text = source.text();
		this.score = source.score();
	}

	private static String cut(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}

	public UUID getMessageId() {
		return messageId;
	}

	public int getPosition() {
		return position;
	}

	public UUID getDocumentId() {
		return documentId;
	}

	public String getDocumentTitle() {
		return documentTitle;
	}

	public String getScope() {
		return scope;
	}

	public String getArticle() {
		return article;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public String getChapter() {
		return chapter;
	}

	public String getText() {
		return text;
	}

	public double getScore() {
		return score;
	}

}
