package az.cyberlaw.rag;

import java.util.List;

import az.cyberlaw.rag.LegalTextSplitter.Chunk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalTextSplitterTest {

	private final LegalTextSplitter splitter = new LegalTextSplitter(2000);

	@Test
	void splitsAzerbaijaniLawByArticle() {
		String text = """
				Fərdi məlumatlar haqqında
				Azərbaycan Respublikasının Qanunu

				Fəsil I
				Ümumi müddəalar

				Maddə 1. Qanunun məqsədi
				Bu Qanun fərdi məlumatların toplanması qaydalarını müəyyən edir.

				Maddə 2. Əsas anlayışlar
				2.1. Bu Qanunda istifadə olunan əsas anlayışlar aşağıdakılardır.

				Fəsil II
				Fərdi məlumatların mühafizəsi

				Maddə 5-1. Məlumatların təhlükəsizliyi
				Operator fərdi məlumatların mühafizəsini təmin edir.
				""";

		List<Chunk> chunks = splitter.split(text);

		assertThat(chunks).extracting(Chunk::article).containsExactly("", "Maddə 1", "Maddə 2", "Maddə 5-1");
		assertThat(chunks.get(1).articleTitle()).isEqualTo("Qanunun məqsədi");
		assertThat(chunks.get(1).chapter()).startsWith("Fəsil I");
		assertThat(chunks.get(3).chapter()).startsWith("Fəsil II");
		// the "Fəsil II" heading belongs to the next article, not to the end of Maddə 2
		assertThat(chunks.get(2).text()).doesNotContain("Fəsil II");
	}

	@Test
	void splitsEnglishArticlesWithTitleOnNextLine() {
		String text = """
				Article 1
				Subject-matter and objectives
				This Regulation lays down rules relating to the protection of natural persons.

				Article 2
				Material scope
				Article 5 of this Regulation applies as well.
				""";

		List<Chunk> chunks = splitter.split(text);

		assertThat(chunks).extracting(Chunk::article).containsExactly("Article 1", "Article 2");
		assertThat(chunks.get(0).articleTitle()).isEqualTo("Subject-matter and objectives");
		// the wrapped cross-reference "Article 5 of ..." must not start a new chunk
		assertThat(chunks.get(1).text()).contains("Article 5 of this Regulation");
	}

	@Test
	void splitsLongArticlesAndRepeatsHeading() {
		String paragraph = "The operator shall implement appropriate technical measures. ".repeat(20);
		String text = "Article 1\nTitle one\n" + (paragraph + "\n\n").repeat(5) + "Article 2\nTitle two\nShort.";

		List<Chunk> chunks = splitter.split(text);

		assertThat(chunks.size()).isGreaterThan(2);
		assertThat(chunks).allSatisfy(c -> assertThat(c.text().length()).isLessThanOrEqualTo(2100));
		assertThat(chunks.get(1).article()).isEqualTo("Article 1");
		assertThat(chunks.get(1).text()).startsWith("Article 1. Title one (cont.)");
	}

	@Test
	void fallsBackToParagraphsWithoutArticles() {
		String text = "Password policy\n\nAll passwords must have at least 12 characters.\n\nMFA is required.";

		List<Chunk> chunks = splitter.split(text);

		assertThat(chunks).hasSize(1);
		assertThat(chunks.get(0).article()).isEmpty();
	}

}
