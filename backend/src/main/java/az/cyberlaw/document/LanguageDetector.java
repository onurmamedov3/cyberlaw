package az.cyberlaw.document;

/** Tiny heuristic: Azerbaijani text is full of letters that English never uses. */
public final class LanguageDetector {

	private static final String AZ_LETTERS = "əƏğĞıİşŞçÇöÖüÜ";

	private LanguageDetector() {
	}

	public static String detect(String text) {
		if (text == null || text.isBlank()) {
			return "en";
		}
		int sample = Math.min(text.length(), 20_000);
		int letters = 0;
		int azLetters = 0;
		for (int i = 0; i < sample; i++) {
			char c = text.charAt(i);
			if (Character.isLetter(c)) {
				letters++;
				if (AZ_LETTERS.indexOf(c) >= 0) {
					azLetters++;
				}
			}
		}
		if (letters == 0) {
			return "en";
		}
		// a single Azerbaijani letter in a short question is already a strong signal
		boolean shortText = letters < 200;
		return (shortText ? azLetters > 0 : azLetters * 100 / letters >= 2) ? "az" : "en";
	}

}
