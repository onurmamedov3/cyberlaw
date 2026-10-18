package az.cyberlaw.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits legal text along its own structure: one chunk per article ("Maddə 5.", "Article 32"),
 * so that every chunk can be cited by article. Long articles are split further by paragraph.
 * Text without recognizable articles (policies, strategies) falls back to paragraph chunks.
 */
public class LegalTextSplitter {

	/**
	 * An article heading at the start of a line: the number must be followed by a dot
	 * ("Maddə 5. Title") or by the end of the line ("Article 5" + title on the next line).
	 * This rejects wrapped cross-references such as "Article 5 of this Regulation".
	 */
	private static final Pattern ARTICLE = Pattern.compile(
			"^[ \\t]*(Maddə|Madde|MADDƏ|Article|ARTICLE|Статья)[ \\t]+(\\d+(?:[-.]\\d+)?)[ \\t]*(?:\\.[ \\t]*(.*))?$",
			Pattern.MULTILINE | Pattern.UNICODE_CASE);

	private static final Pattern CHAPTER = Pattern.compile(
			"^[ \\t]*((?:Fəsil|FƏSİL|Fesil|Chapter|CHAPTER|Глава|Bölmə|BÖLMƏ|Section|SECTION)[ \\t]+[IVXLC\\d]+\\b.*)$",
			Pattern.MULTILINE | Pattern.UNICODE_CASE);

	private static final int MAX_TITLE = 200;

	private final int maxChars;

	public LegalTextSplitter(int maxChars) {
		this.maxChars = Math.max(500, maxChars);
	}

	public record Chunk(int index, String text, String article, String articleTitle, String chapter) {
	}

	private record Heading(int start, int bodyStart, String article, String title) {
	}

	private record ChapterMark(int start, String title) {
	}

	public List<Chunk> split(String rawText) {
		String text = normalize(rawText);
		if (text.isBlank()) {
			return List.of();
		}
		List<Heading> headings = findHeadings(text);
		List<ChapterMark> chapters = findChapters(text);
		List<Chunk> chunks = new ArrayList<>();

		if (headings.size() < 2) {
			for (String part : splitBySize(text)) {
				chunks.add(new Chunk(chunks.size(), part, "", "", ""));
			}
			return chunks;
		}

		String preamble = text.substring(0, headings.get(0).start()).strip();
		if (!preamble.isBlank()) {
			for (String part : splitBySize(preamble)) {
				chunks.add(new Chunk(chunks.size(), part, "", "", ""));
			}
		}

		for (int i = 0; i < headings.size(); i++) {
			Heading h = headings.get(i);
			int end = (i + 1 < headings.size()) ? headings.get(i + 1).start() : text.length();
			String section = text.substring(h.start(), end).strip();
			String chapter = chapterAt(chapters, h.start());
			// a chapter heading right before the next article belongs to that next article
			section = stripTrailingChapter(section);

			List<String> parts = splitBySize(section);
			String headingLine = h.article() + (h.title().isEmpty() ? "" : ". " + h.title());
			for (int p = 0; p < parts.size(); p++) {
				String part = parts.get(p);
				if (p > 0) {
					part = headingLine + " (cont.)\n" + part;
				}
				chunks.add(new Chunk(chunks.size(), part, h.article(), h.title(), chapter));
			}
		}
		return chunks;
	}

	private List<Heading> findHeadings(String text) {
		List<Heading> headings = new ArrayList<>();
		Matcher m = ARTICLE.matcher(text);
		while (m.find()) {
			String word = m.group(1);
			String label = (word.equalsIgnoreCase("madde") || word.equalsIgnoreCase("maddə")) ? "Maddə"
					: word.equalsIgnoreCase("article") ? "Article" : word;
			String title = m.group(3) != null ? m.group(3).strip() : "";
			if (title.isEmpty()) {
				title = nextLine(text, m.end());
			}
			headings.add(new Heading(m.start(), m.end(), label + " " + m.group(2), truncate(title)));
		}
		return headings;
	}

	private static List<ChapterMark> findChapters(String text) {
		List<ChapterMark> chapters = new ArrayList<>();
		Matcher m = CHAPTER.matcher(text);
		while (m.find()) {
			String title = m.group(1).strip();
			// "Fəsil I" often has its name on the following line
			if (title.split("\\s+").length <= 2) {
				String next = nextLine(text, m.end());
				if (!next.isEmpty() && !ARTICLE.matcher(next).matches()) {
					title = title + ". " + next;
				}
			}
			chapters.add(new ChapterMark(m.start(), truncate(title)));
		}
		return chapters;
	}

	private static String chapterAt(List<ChapterMark> chapters, int position) {
		String current = "";
		for (ChapterMark c : chapters) {
			if (c.start() > position) {
				break;
			}
			current = c.title();
		}
		return current;
	}

	private static String stripTrailingChapter(String section) {
		Matcher m = CHAPTER.matcher(section);
		int cut = -1;
		while (m.find()) {
			if (m.start() > 0) {
				cut = m.start();
			}
		}
		// only cut if the chapter heading is in the last few lines (i.e. it introduces the next part)
		if (cut > 0 && section.length() - cut < 300) {
			return section.substring(0, cut).strip();
		}
		return section;
	}

	/** Keeps paragraphs together; only very long paragraphs are cut at sentence boundaries. */
	List<String> splitBySize(String text) {
		List<String> result = new ArrayList<>();
		if (text.length() <= maxChars) {
			result.add(text);
			return result;
		}
		StringBuilder current = new StringBuilder();
		for (String paragraph : text.split("\\n\\s*\\n|\\n(?=\\s*(?:\\d+[.)]|[a-zəğıöüşç][.)]|•|-)\\s)")) {
			String para = paragraph.strip();
			if (para.isEmpty()) {
				continue;
			}
			if (para.length() > maxChars) {
				flush(current, result);
				result.addAll(splitLongParagraph(para));
				continue;
			}
			if (current.length() + para.length() + 2 > maxChars) {
				flush(current, result);
			}
			if (!current.isEmpty()) {
				current.append("\n\n");
			}
			current.append(para);
		}
		flush(current, result);
		return result;
	}

	private List<String> splitLongParagraph(String para) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String sentence : para.split("(?<=[.;:!?])\\s+")) {
			if (sentence.length() > maxChars) {
				flush(current, parts);
				for (int i = 0; i < sentence.length(); i += maxChars) {
					parts.add(sentence.substring(i, Math.min(sentence.length(), i + maxChars)));
				}
				continue;
			}
			if (current.length() + sentence.length() + 1 > maxChars) {
				flush(current, parts);
			}
			if (!current.isEmpty()) {
				current.append(' ');
			}
			current.append(sentence);
		}
		flush(current, parts);
		return parts;
	}

	private static void flush(StringBuilder current, List<String> target) {
		if (!current.isEmpty()) {
			target.add(current.toString());
			current.setLength(0);
		}
	}

	private static String nextLine(String text, int from) {
		int i = from;
		while (i < text.length() && (text.charAt(i) == '\n' || text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
			i++;
		}
		int end = text.indexOf('\n', i);
		String line = text.substring(i, end < 0 ? text.length() : end).strip();
		return line.length() <= MAX_TITLE ? line : "";
	}

	private static String truncate(String s) {
		return s.length() <= MAX_TITLE ? s : s.substring(0, MAX_TITLE);
	}

	static String normalize(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("\r\n", "\n")
			.replace('\r', '\n')
			.replace(' ', ' ')
			.replaceAll("[ \\t]+\\n", "\n")
			.replaceAll("\\n{3,}", "\n\n")
			.strip();
	}

}
