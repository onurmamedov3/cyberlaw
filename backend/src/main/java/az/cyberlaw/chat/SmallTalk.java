package az.cyberlaw.chat;

import java.util.Locale;
import java.util.Set;

/**
 * Detects messages that are only a greeting, thanks or goodbye ("Salam", "sağ ol", "thank you very much").
 * They skip the document search: their embedding has no legal meaning and would only match random chunks.
 * Every word must be small talk, so "Salam, Maddə 5 nə deyir?" still goes through the normal RAG path.
 */
final class SmallTalk {

	private static final int MAX_WORDS = 6;

	/** Locale.ROOT lower-cases "İ" to "i" + U+0307; dropping the dot makes "İ" and "I" both "i". */
	private static final char COMBINING_DOT = (char) 0x0307;

	private static final Set<String> WORDS = Set.of(
			// Azerbaijani
			"salam", "salamlar", "aleykum", "əleykum", "əleyküm", "sağ", "sağol", "ol", "olun", "təşəkkür",
			"təşəkkürlər", "edirəm", "edirik", "minnətdaram", "çox", "sabahınız", "axşamınız", "gecəniz", "günortanız",
			"xeyir", "xeyirli", "necəsən", "necəsiniz", "yaxşı", "əla", "oldu", "aydındır", "aydın", "hə", "bəli",
			"hələlik", "görüşənədək", "sağolun", "xoş", "gəldin", "gördük",
			// English
			"hi", "hello", "hey", "yo", "greetings", "thanks", "thank", "thx", "ty", "you", "very", "much", "so",
			"a", "lot", "appreciate", "it", "ok", "okay", "oke", "cool", "great", "nice", "perfect", "awesome",
			"good", "morning", "afternoon", "evening", "night", "bye", "goodbye", "see", "later", "cheers", "how",
			"are", "there", "got");

	private SmallTalk() {
	}

	static boolean matches(String message) {
		if (message == null) {
			return false;
		}
		String normalized = message.toLowerCase(Locale.ROOT).replace(String.valueOf(COMBINING_DOT), "").strip();
		if (normalized.isEmpty()) {
			return false;
		}
		String[] words = normalized.split("[^\\p{L}\\p{N}]+");
		int count = 0;
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (++count > MAX_WORDS || !WORDS.contains(word)) {
				return false;
			}
		}
		return count > 0;
	}

}
