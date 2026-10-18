package az.cyberlaw.user;

import java.util.Locale;

import az.cyberlaw.config.CyberLawProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the admin account from .env on first start. */
@Component
public class AdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final AppUserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final CyberLawProperties props;

	public AdminSeeder(AppUserRepository users, PasswordEncoder passwordEncoder, CyberLawProperties props) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.props = props;
	}

	@Override
	public void run(ApplicationArguments args) {
		String email = props.admin().email().trim().toLowerCase(Locale.ROOT);
		users.findByEmail(email).ifPresentOrElse(user -> {
			if (user.getRole() != Role.ADMIN) {
				user.setRole(Role.ADMIN);
				users.save(user);
			}
		}, () -> {
			users.save(new AppUser(email, passwordEncoder.encode(props.admin().password()), "Admin", Role.ADMIN));
			log.info("Created admin account {}", email);
		});
		if (!props.aiConfigured()) {
			log.warn("GEMINI_API_KEY is not set. Paste your key into the .env file and restart.");
		}
		String secret = props.jwt().secret();
		if (secret.length() < 32 || secret.toLowerCase(Locale.ROOT).contains("change")) {
			log.warn("JWT_SECRET in .env looks like a placeholder. Replace it with a long random string.");
		}
	}

}
