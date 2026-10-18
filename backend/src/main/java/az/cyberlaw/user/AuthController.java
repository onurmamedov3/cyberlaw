package az.cyberlaw.user;

import java.util.Locale;

import az.cyberlaw.common.ApiException;
import az.cyberlaw.security.CurrentUser;
import az.cyberlaw.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AppUserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 100) String password,
			@NotBlank @Size(max = 100) String displayName) {
	}

	public record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	public record UserDto(String id, String email, String displayName, String role) {
		static UserDto of(AppUser user) {
			return new UserDto(user.getId().toString(), user.getEmail(), user.getDisplayName(), user.getRole().name());
		}
	}

	public record AuthResponse(String token, UserDto user) {
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		String email = normalize(request.email());
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("An account with this email already exists.");
		}
		AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()),
				request.displayName().trim(), Role.USER));
		return new AuthResponse(jwtService.issueToken(user), UserDto.of(user));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		AppUser user = users.findByEmail(normalize(request.email()))
			.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password."));
		return new AuthResponse(jwtService.issueToken(user), UserDto.of(user));
	}

	@GetMapping("/me")
	public UserDto me(@AuthenticationPrincipal Jwt jwt) {
		return users.findById(CurrentUser.of(jwt).id())
			.map(UserDto::of)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists."));
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
