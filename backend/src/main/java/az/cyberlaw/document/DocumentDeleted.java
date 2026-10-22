package az.cyberlaw.document;

import java.util.UUID;

/** Published after a document and its chunks are deleted (by its owner, the admin, or the expiry job). */
public record DocumentDeleted(UUID documentId, DocumentScope scope) {
}
