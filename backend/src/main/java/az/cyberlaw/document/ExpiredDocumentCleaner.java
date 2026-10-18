package az.cyberlaw.document;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes private uploads after their time-to-live (default 7 days). */
@Component
public class ExpiredDocumentCleaner {

	private static final Logger log = LoggerFactory.getLogger(ExpiredDocumentCleaner.class);

	private final LibraryDocumentRepository documents;

	private final DocumentService documentService;

	public ExpiredDocumentCleaner(LibraryDocumentRepository documents, DocumentService documentService) {
		this.documents = documents;
		this.documentService = documentService;
	}

	@Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
	public void deleteExpired() {
		var expired = documents.findByScopeAndExpiresAtBefore(DocumentScope.PRIVATE, Instant.now());
		expired.forEach(documentService::deleteWithChunks);
		if (!expired.isEmpty()) {
			log.info("Deleted {} expired private document(s).", expired.size());
		}
	}

}
