package com.cms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cms.config.JwtProperties;
import com.cms.entity.User;
import com.cms.support.TestData;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");
    private static final String SECRET = randomSecret(48);

    private final JwtProperties properties = new JwtProperties(SECRET, 15, 7);
    private final JwtService service = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

    private static String randomSecret(int bytes) {
        byte[] data = new byte[bytes];
        new SecureRandom().nextBytes(data);
        return Base64.getEncoder().encodeToString(data);
    }

    private static User user() {
        User user = TestData.user(42L, "jane@example.com", "+15551234567", "hash");
        user.setTokenVersion(3);
        return user;
    }

    @Test
    void generatedToken_roundTripsUserIdAndTokenVersion() {
        AccessTokenClaims claims = service.parseAccessToken(service.generateAccessToken(user()));

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    @Test
    void accessTokenTtl_matchesConfiguration() {
        assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(15 * 60L);
    }

    @Test
    void expiredToken_throwsExpiredJwtException() {
        JwtService issuedAnHourAgo = new JwtService(properties,
                Clock.fixed(NOW.minus(Duration.ofHours(1)), ZoneOffset.UTC));
        String token = issuedAnHourAgo.generateAccessToken(user());

        assertThatThrownBy(() -> service.parseAccessToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        JwtService other = new JwtService(new JwtProperties(randomSecret(48), 15, 7), Clock.fixed(NOW, ZoneOffset.UTC));
        String foreignToken = other.generateAccessToken(user());

        assertThatThrownBy(() -> service.parseAccessToken(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedPayload_isRejected() {
        String[] parts = service.generateAccessToken(user()).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"tv\":3,\"iss\":\"contact-management-system\"}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> service.parseAccessToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageToken_isRejected() {
        assertThatThrownBy(() -> service.parseAccessToken("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_rejectsSecretThatIsNotBase64() {
        JwtProperties bad = new JwtProperties("***definitely not base64***", 15, 7);
        assertThatThrownBy(() -> new JwtService(bad, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void constructor_rejectsSecretShorterThan256Bits() {
        JwtProperties shortSecret = new JwtProperties(randomSecret(16), 15, 7);
        assertThatThrownBy(() -> new JwtService(shortSecret, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
