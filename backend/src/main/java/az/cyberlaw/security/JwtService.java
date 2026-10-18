package az.cyberlaw.security;

import java.time.Instant;
import java.util.List;

import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.user.AppUser;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder encoder;

	private final CyberLawProperties props;

	public JwtService(JwtEncoder encoder, CyberLawProperties props) {
		this.encoder = encoder;
		this.props = props;
	}

	public String issueToken(AppUser user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("cyberlaw")
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(props.jwt().expiration()))
			.claim("email", user.getEmail())
			.claim("name", user.getDisplayName())
			.claim("roles", List.of(user.getRole().name()))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
