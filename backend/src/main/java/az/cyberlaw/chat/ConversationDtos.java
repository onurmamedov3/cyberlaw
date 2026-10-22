package az.cyberlaw.chat;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ConversationDtos {

	private ConversationDtos() {
	}

	public record ConversationDto(String id, String title, String scope, Instant updatedAt) {
		static ConversationDto of(Conversation c) {
			return new ConversationDto(c.getId().toString(), c.getTitle(), c.getScope().name(), c.getUpdatedAt());
		}
	}

	/** The same shape as a live answer's sources; deleted = the private document was deleted, its text is gone. */
	public record SourceDto(int index, String documentId, String documentTitle, String scope, String article,
			String articleTitle, String chapter, String text, double score, boolean deleted) {
		static SourceDto of(MessageSource s) {
			return new SourceDto(s.getPosition(), s.getDocumentId() != null ? s.getDocumentId().toString() : "",
					s.getDocumentTitle(), s.getScope(), nz(s.getArticle()), nz(s.getArticleTitle()),
					nz(s.getChapter()), nz(s.getText()), s.getScore(), s.getText() == null);
		}

		private static String nz(String s) {
			return s != null ? s : "";
		}
	}

	/** role: "user" or "assistant", like the frontend's ChatMessage. */
	public record MessageDto(String id, String role, String text, List<SourceDto> sources, Instant createdAt) {
	}

	public record ConversationDetailDto(ConversationDto conversation, List<MessageDto> messages) {
	}

	public record RenameRequest(@NotBlank @Size(max = 200) String title) {
	}

}
