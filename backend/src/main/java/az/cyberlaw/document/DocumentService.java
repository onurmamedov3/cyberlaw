package az.cyberlaw.document;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

	public static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "doc", "txt", "md", "html", "htm",
			"rtf", "odt");

	private final LibraryDocumentRepository documents;

	private final IngestionService ingestion;

	private final CyberLawProperties props;

	public DocumentService(LibraryDocumentRepository documents, IngestionService ingestion, CyberLawProperties props) {
		this.documents = documents;
		this.ingestion = ingestion;
		this.props = props;
	}

	public List<DocumentDto> listVisible(CurrentUser user) {
		return documents.findVisibleTo(user.id()).stream().map(d -> DocumentDto.of(d, user.id())).toList();
	}

	public DocumentDto upload(CurrentUser user, MultipartFile file, DocumentScope scope, String title) {
		if (!props.aiConfigured()) {
			throw new AiException(AiErrorCode.NOT_CONFIGURED, AiException.describe(AiErrorCode.NOT_CONFIGURED));
		}
		if (file == null || file.isEmpty()) {
			throw new ApiException(ErrorCode.FILE_MISSING);
		}
		String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
		if (!ALLOWED_EXTENSIONS.contains(extension(filename))) {
			throw new ApiException(ErrorCode.FILE_TYPE);
		}
		if (scope == DocumentScope.OFFICIAL && !user.admin()) {
			throw new ApiException(ErrorCode.OFFICIAL_ADMIN_ONLY);
		}
		if (scope == DocumentScope.PRIVATE
				&& documents.countByOwnerIdAndScope(user.id(), DocumentScope.PRIVATE) >= props.uploads()
					.maxPrivateDocuments()) {
			throw new ApiException(ErrorCode.DOCUMENT_LIMIT, Map.of("max", props.uploads().maxPrivateDocuments()));
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (IOException ex) {
			throw new ApiException(ErrorCode.FILE_UNREADABLE);
		}
		String sha = sha256(bytes);
		boolean duplicate = scope == DocumentScope.OFFICIAL ? documents.existsBySha256AndScope(sha, scope)
				: documents.existsBySha256AndOwnerId(sha, user.id());
		if (duplicate) {
			throw new ApiException(ErrorCode.DUPLICATE_FILE);
		}

		String finalTitle = (title != null && !title.isBlank()) ? title.trim() : titleFromFilename(filename);
		Instant expiresAt = scope == DocumentScope.PRIVATE ? Instant.now().plus(props.uploads().privateTtl()) : null;
		UUID ownerId = scope == DocumentScope.PRIVATE ? user.id() : null;
		LibraryDocument doc = documents.save(new LibraryDocument(truncate(finalTitle, 300), truncate(filename, 300),
				file.getContentType(), bytes.length, sha, scope, ownerId, expiresAt));
		ingestion.ingest(doc.getId(), bytes);
		return DocumentDto.of(doc, user.id());
	}

	/** Used by the docs/ folder loader for the Official Library. */
	public LibraryDocument registerOfficial(String filename, byte[] bytes, String sha) {
		LibraryDocument doc = documents.save(new LibraryDocument(truncate(titleFromFilename(filename), 300),
				truncate(filename, 300), null, bytes.length, sha, DocumentScope.OFFICIAL, null, null));
		ingestion.ingest(doc.getId(), bytes);
		return doc;
	}

	public void delete(CurrentUser user, UUID id) {
		LibraryDocument doc = documents.findById(id).orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
		boolean allowed = doc.getScope() == DocumentScope.OFFICIAL ? user.admin()
				: user.id().equals(doc.getOwnerId()) || user.admin();
		if (!allowed) {
			throw new ApiException(ErrorCode.DOCUMENT_DELETE_FORBIDDEN);
		}
		deleteWithChunks(doc);
	}

	public void deleteWithChunks(LibraryDocument doc) {
		ingestion.deleteChunks(doc.getId());
		documents.delete(doc);
	}

	public LibraryDocument getVisible(CurrentUser user, UUID id) {
		LibraryDocument doc = documents.findById(id).orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
		if (!doc.isVisibleTo(user.id())) {
			throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND);
		}
		return doc;
	}

	public static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static String extension(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	static String titleFromFilename(String filename) {
		int dot = filename.lastIndexOf('.');
		String base = dot > 0 ? filename.substring(0, dot) : filename;
		return base.replace('_', ' ').replaceAll("\\s+", " ").trim();
	}

	private static String truncate(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}

}
