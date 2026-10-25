package az.cyberlaw.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import az.cyberlaw.ai.AiException;
import az.cyberlaw.ai.AiRetry;
import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.rag.LegalTextSplitter;
import az.cyberlaw.rag.LegalTextSplitter.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Parses a file, splits it by article, embeds the chunks with Gemini and stores them in PGVector. */
@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private static final int EMBED_BATCH = 20;

	private final LibraryDocumentRepository documents;

	private final VectorStore vectorStore;

	private final LegalTextSplitter splitter;

	public IngestionService(LibraryDocumentRepository documents, VectorStore vectorStore, CyberLawProperties props) {
		this.documents = documents;
		this.vectorStore = vectorStore;
		this.splitter = new LegalTextSplitter(props.rag().chunkMaxChars());
	}

	@Async("ingestionExecutor")
	public void ingest(UUID documentId, byte[] bytes) {
		LibraryDocument doc = documents.findById(documentId).orElse(null);
		if (doc == null) {
			return;
		}
		long started = System.currentTimeMillis();
		try {
			String text = extractText(bytes, doc.getOriginalFilename());
			if (text.isBlank()) {
				throw new ApiException(ErrorCode.NO_TEXT);
			}
			String language = LanguageDetector.detect(text);
			List<Chunk> chunks = splitter.split(text);

			deleteChunks(documentId);
			List<Document> batch = new ArrayList<>();
			for (Chunk chunk : chunks) {
				batch.add(toVectorDocument(doc, chunk, language));
				if (batch.size() == EMBED_BATCH) {
					store(doc, batch);
				}
			}
			store(doc, batch);

			doc.markReady(chunks.size(), language);
			saveIfStillExists(doc);
			log.info("Indexed '{}' ({} chunks, {}) in {} ms", doc.getTitle(), chunks.size(), language,
					System.currentTimeMillis() - started);
		}
		catch (ApiException ex) {
			log.warn("Indexing '{}' failed: {}", doc.getTitle(), ex.getMessage());
			fail(doc, ex.getCode().name(), ex.getMessage());
		}
		catch (AiException ex) {
			log.warn("Indexing '{}' failed: {}", doc.getTitle(), ex.getMessage());
			fail(doc, ex.getCode().name(), ex.getMessage());
		}
		catch (Exception ex) {
			log.error("Indexing '{}' failed", doc.getTitle(), ex);
			fail(doc, ErrorCode.INDEXING_FAILED.name(), ErrorCode.INDEXING_FAILED.message());
		}
	}

	private void fail(LibraryDocument doc, String code, String message) {
		deleteChunks(doc.getId());
		doc.markFailed(code, message);
		saveIfStillExists(doc);
	}

	/** The user may delete a document while it is still being indexed. */
	private void saveIfStillExists(LibraryDocument doc) {
		if (documents.existsById(doc.getId())) {
			documents.save(doc);
		}
		else {
			deleteChunks(doc.getId());
		}
	}

	private void store(LibraryDocument doc, List<Document> batch) {
		if (batch.isEmpty()) {
			return;
		}
		List<Document> copy = List.copyOf(batch);
		AiRetry.run("embed " + doc.getTitle(), () -> vectorStore.add(copy));
		batch.clear();
	}

	public void deleteChunks(UUID documentId) {
		vectorStore.delete("documentId == '" + documentId + "'");
	}

	private static String extractText(byte[] bytes, String filename) {
		ByteArrayResource resource = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
		try {
			return new TikaDocumentReader(resource).get()
				.stream()
				.map(Document::getText)
				.filter(t -> t != null && !t.isBlank())
				.collect(Collectors.joining("\n\n"));
		}
		catch (RuntimeException ex) {
			// damaged, encrypted or mislabelled file
			throw new ApiException(ErrorCode.FILE_UNREADABLE);
		}
	}

	private static Document toVectorDocument(LibraryDocument doc, Chunk chunk, String language) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("documentId", doc.getId().toString());
		metadata.put("title", doc.getTitle());
		metadata.put("scope", doc.getScope().name());
		metadata.put("ownerId", doc.getOwnerId() != null ? doc.getOwnerId().toString() : "");
		metadata.put("language", language);
		metadata.put("article", chunk.article());
		metadata.put("articleTitle", chunk.articleTitle());
		metadata.put("chapter", chunk.chapter());
		metadata.put("chunkIndex", chunk.index());
		// the title line gives the embedding context for short chunks; RetrievalService strips it again
		return new Document(doc.getTitle() + "\n" + chunk.text(), metadata);
	}

}
