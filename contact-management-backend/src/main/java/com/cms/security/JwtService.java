package com.cms.security;

import com.cms.config.JwtProperties;
import com.cms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Creates and verifies short-lived HS256 access tokens. */
@Service
public class JwtService {

    private static final String ISSUER = "contact-management-system";
    private static final String CLAIM_TOKEN_VERSION = "tv";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = buildKey(properties.secret());
        this.accessTtl = Duration.ofMinutes(properties.accessTtlMinutes());
        this.clock = clock;
    }

    private static SecretKey buildKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be Base64 encoded, e.g. generated with: openssl rand -base64 48", e);
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must decode to at least 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * Verifies signature, issuer and expiry.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException when the token has expired
     * @throws JwtException                        for any other invalid token
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        if (tokenVersion == null || claims.getSubject() == null) {
            throw new JwtException("Token is missing required claims");
        }
        try {
            return new AccessTokenClaims(Long.parseLong(claims.getSubject()), tokenVersion);
        } catch (NumberFormatException e) {
            throw new JwtException("Token subject is not a valid user id", e);
        }
    }
}
