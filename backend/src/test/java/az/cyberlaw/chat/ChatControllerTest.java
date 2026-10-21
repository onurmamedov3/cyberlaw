package az.cyberlaw.chat;

import java.util.List;
import java.util.UUID;

import az.cyberlaw.common.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The chat request exactly as the browser sends it, through JSON parsing, validation and the chat limit. */
class ChatControllerTest {

	private final ChatService chatService = mock(ChatService.class);

	private final MockMvc mvc = MockMvcBuilders
		.standaloneSetup(new ChatController(chatService, new ChatRateLimiter(5, System::currentTimeMillis)))
		.setControllerAdvice(new GlobalExceptionHandler(DataSize.ofMegabytes(10)))
		.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
		.build();

	@BeforeEach
	void logIn() {
		Jwt jwt = Jwt.withTokenValue("token")
			.header("alg", "HS256")
			.subject(UUID.randomUUID().toString())
			.claim("roles", List.of("USER"))
			.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
		when(chatService.stream(any(), any())).thenReturn(Flux.empty());
	}

	@AfterEach
	void logOut() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void optionalFieldsMayBeLeftOut() throws Exception {
		ask("{\"question\": \"What is GDPR?\", \"conversationId\": \"c1\"}").andExpect(status().isOk());
	}

	@Test
	void aBlankQuestionGetsItsOwnCode() throws Exception {
		ask("{\"question\": \"  \", \"conversationId\": \"c1\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
	}

	@Test
	void theSixthQuestionWithinAMinuteHasToWait() throws Exception {
		for (int i = 0; i < 5; i++) {
			ask("{\"question\": \"Question " + i + "\", \"conversationId\": \"c1\"}").andExpect(status().isOk());
		}
		ask("{\"question\": \"One more\", \"conversationId\": \"c1\"}").andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("TOO_MANY_QUESTIONS"))
			.andExpect(jsonPath("$.params.seconds").isNumber());
	}

	private ResultActions ask(String json) throws Exception {
		return mvc.perform(post("/api/chat/stream").contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.content(json));
	}

}
