package az.cyberlaw.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import az.cyberlaw.config.CyberLawProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On startup: cleans up documents whose indexing was interrupted by a restart,
 * then indexes every new file in the docs/ folder into the Official Library.
 */
@Component
public class DocsFolderLoader {

	private static final Logger log = LoggerFactory.getLogger(DocsFolderLoader.class);

	private final LibraryDocumentRepository documents;

	private final DocumentService documentService;

	private final CyberLawProperties props;

	public DocsFolderLoader(LibraryDocumentRepository documents, DocumentService documentService,
			CyberLawProperties props) {
		this.documents = documents;
		this.documentService = documentService;
		this.props = props;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		recoverInterrupted();
		if (!props.aiConfigured()) {
			log.warn("Skipping docs/ folder indexing: GEMINI_API_KEY is not configured.");
			return;
		}
		Path folder = Path.of(props.docsFolder()).toAbsolutePath().normalize();
		if (!Files.isDirectory(folder)) {
			log.info("Docs folder {} does not exist, nothing to index.", folder);
			return;
		}
		int queued = 0;
		try (Stream<Path> files = Files.walk(folder, 3)) {
			for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
				String name = file.getFileName().toString();
				if (name.equalsIgnoreCase("README.md")
						|| !DocumentService.ALLOWED_EXTENSIONS.contains(DocumentService.extension(name))) {
					continue;
				}
				byte[] bytes = Files.readAllBytes(file);
				String sha = DocumentService.sha256(bytes);
				if (documents.existsBySha256AndScope(sha, DocumentScope.OFFICIAL)) {
					continue;
				}
				documentService.registerOfficial(name, bytes, sha);
				queued++;
			}
		}
		catch (IOException ex) {
			log.warn("Could not scan docs folder {}: {}", folder, ex.getMessage());
		}
		log.info("Docs folder {}: {} new file(s) queued for indexing.", folder, queued);
	}

	private void recoverInterrupted() {
		for (LibraryDocument doc : documents.findByStatusIn(List.of(DocumentStatus.PROCESSING))) {
			if (doc.getScope() == DocumentScope.OFFICIAL && doc.getOwnerId() == null) {
				// official files are re-indexed from docs/ (or re-uploaded by the admin)
				documentService.deleteWithChunks(doc);
			}
			else {
				doc.markFailed("Indexing was interrupted by a server restart. Please upload the file again.");
				documents.save(doc);
			}
		}
		for (LibraryDocument doc : documents.findByScopeAndRequirementsStatus(DocumentScope.OFFICIAL,
				RequirementsStatus.EXTRACTING)) {
			doc.setRequirementsStatus(RequirementsStatus.FAILED);
			documents.save(doc);
		}
	}

}
