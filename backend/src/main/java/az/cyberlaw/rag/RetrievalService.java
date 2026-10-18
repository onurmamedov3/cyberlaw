package az.cyberlaw.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import az.cyberlaw.ai.AiRetry;
import az.cyberlaw.config.CyberLawProperties;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

	private final VectorStore vectorStore;

	private final CyberLawProperties props;

	public RetrievalService(VectorStore vectorStore, CyberLawProperties props) {
		this.vectorStore = vectorStore;
		this.props = props;
	}

	/** Chat search, restricted to what this user may see. */
	public List<Source> search(String query, UUID userId, SearchScope scope) {
		FilterExpressionBuilder b = new FilterExpressionBuilder();
		FilterExpressionBuilder.Op official = b.eq("scope", "OFFICIAL");
		FilterExpressionBuilder.Op mine = b.and(b.eq("scope", "PRIVATE"), b.eq("ownerId", userId.toString()));
		Filter.Expression filter = switch (scope) {
			case OFFICIAL -> official.build();
			case MINE -> mine.build();
			case BOTH -> b.or(official, mine).build();
		};
		return search(query, filter, props.rag().topK());
	}

	/** Search inside a single document (used by the compliance checker). */
	public List<Source> searchInDocument(String query, UUID documentId, int topK) {
		Filter.Expression filter = new FilterExpressionBuilder().eq("documentId", documentId.toString()).build();
		return search(query, filter, topK);
	}

	private List<Source> search(String query, Filter.Expression filter, int topK) {
		SearchRequest request = SearchRequest.builder()
			.query(query)
			.topK(topK)
			.similarityThreshold(props.rag().similarityThreshold())
			.filterExpression(filter)
			.build();
		List<Document> hits = AiRetry.call("search", () -> vectorStore.similaritySearch(request));
		List<Source> sources = new ArrayList<>();
		for (Document hit : hits) {
			sources.add(toSource(sources.size() + 1, hit));
		}
		return sources;
	}

	private static Source toSource(int index, Document doc) {
		Map<String, Object> m = doc.getMetadata();
		String title = str(m, "title");
		String text = doc.getText() != null ? doc.getText() : "";
		// IngestionService prefixes every chunk with the document title for better embeddings
		if (text.startsWith(title + "\n")) {
			text = text.substring(title.length() + 1);
		}
		return new Source(index, str(m, "documentId"), title, str(m, "scope"), str(m, "article"),
				str(m, "articleTitle"), str(m, "chapter"), text, doc.getScore() != null ? doc.getScore() : 0.0);
	}

	private static String str(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		return value != null ? value.toString() : "";
	}

}
