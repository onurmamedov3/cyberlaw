package az.cyberlaw.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(String id, String title, String filename, String scope, String status, String errorCode,
		String errorMessage, int chunkCount, String language, long sizeBytes, Instant createdAt, Instant expiresAt,
		String requirementsStatus, boolean mine) {

	public static DocumentDto of(LibraryDocument d, UUID viewerId) {
		return new DocumentDto(d.getId().toString(), d.getTitle(), d.getOriginalFilename(), d.getScope().name(),
				d.getStatus().name(), d.getErrorCode(), d.getErrorMessage(), d.getChunkCount(), d.getLanguage(),
				d.getSizeBytes(), d.getCreatedAt(), d.getExpiresAt(), d.getRequirementsStatus().name(),
				viewerId != null && viewerId.equals(d.getOwnerId()));
	}

}
