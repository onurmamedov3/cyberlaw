package az.cyberlaw.chat;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The [1], [2] markers the model writes after a statement (the same pattern as citedIndexes() in the frontend). */
final class Citations {

	private static final Pattern MARKER = Pattern.compile("\\[(\\d{1,2})\\](?!\\()");

	private static final String MARKER_WITH_SPACE = "\\s*\\[\\d{1,2}\\](?!\\()";

	private Citations() {
	}

	/** "… [1] … [2][3]" → {1, 2, 3} */
	static Set<Integer> cited(String text) {
		Matcher m = MARKER.matcher(text);
		return m.results().map(r -> Integer.parseInt(r.group(1))).collect(Collectors.toSet());
	}

	static String strip(String text) {
		return text.replaceAll(MARKER_WITH_SPACE, "");
	}

}
