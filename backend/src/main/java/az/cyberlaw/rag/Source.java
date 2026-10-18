package az.cyberlaw.rag;

/** One retrieved chunk, numbered so the model can cite it as [index]. */
public record Source(int index, String documentId, String documentTitle, String scope, String article,
		String articleTitle, String chapter, String text, double score) {

	/** "Law title — Maddə 5. Title" */
	public String label() {
		StringBuilder sb = new StringBuilder(documentTitle);
		if (!article.isEmpty()) {
			sb.append(" — ").append(article);
			if (!articleTitle.isEmpty()) {
				sb.append(". ").append(articleTitle);
			}
		}
		return sb.toString();
	}

}
