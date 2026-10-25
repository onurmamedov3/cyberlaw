package az.cyberlaw.common;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
		.setControllerAdvice(new GlobalExceptionHandler(DataSize.ofMegabytes(10)))
		.build();

	@Test
	void apiErrorsKeepTheirCodeAndParams() throws Exception {
		mvc.perform(get("/limit"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DOCUMENT_LIMIT"))
			.andExpect(jsonPath("$.params.max").value(5))
			.andExpect(jsonPath("$.message").value("You can keep at most 5 private documents. Delete one first."));
	}

	@Test
	void anIdThatIsNotAUuidIsABadRequest() throws Exception {
		mvc.perform(get("/documents/not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void malformedJsonIsABadRequest() throws Exception {
		mvc.perform(post("/form").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void anInvalidFieldGetsItsOwnCode() throws Exception {
		mvc.perform(post("/form").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\": \" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DISPLAY_NAME"));
	}

	@Test
	void unexpectedErrorsHideTheirDetails() throws Exception {
		mvc.perform(get("/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL"))
			.andExpect(content().string(not(containsString("secret"))));
	}

	@Test
	void aUniqueConstraintIsAConflict() throws Exception {
		mvc.perform(get("/duplicate"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void aStreamThatFailsBeforeStartingAnswersWithJson() throws Exception {
		mvc.perform(post("/stream").accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(status().isTooManyRequests())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("TOO_MANY_QUESTIONS"))
			.andExpect(jsonPath("$.params.seconds").value(12));
	}

	@Test
	void unknownFieldsFallBackToTheGenericValidationCode() {
		org.assertj.core.api.Assertions.assertThat(ErrorCode.forField("displayName"))
			.isEqualTo(ErrorCode.INVALID_DISPLAY_NAME);
		org.assertj.core.api.Assertions.assertThat(ErrorCode.forField("policyDocumentId"))
			.isEqualTo(ErrorCode.VALIDATION);
	}

	@RestController
	static class TestController {

		record Form(@NotBlank String displayName) {
		}

		@GetMapping("/limit")
		void limit() {
			throw new ApiException(ErrorCode.DOCUMENT_LIMIT, Map.of("max", 5));
		}

		@GetMapping("/documents/{id}")
		void document(@PathVariable UUID id) {
		}

		@PostMapping("/form")
		void form(@Valid @RequestBody Form form) {
		}

		@GetMapping("/boom")
		void boom() {
			throw new IllegalStateException("secret internal detail");
		}

		@GetMapping("/duplicate")
		void duplicate() {
			throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
		}

		@PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		Flux<String> stream() {
			throw new ApiException(ErrorCode.TOO_MANY_QUESTIONS, Map.of("seconds", 12));
		}

	}

}
