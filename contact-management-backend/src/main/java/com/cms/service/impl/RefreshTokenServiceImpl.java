package com.cms.service.impl;

import com.cms.config.JwtProperties;
import com.cms.entity.RefreshToken;
import com.cms.exception.UnauthorizedException;
import com.cms.repository.RefreshTokenRepository;
import com.cms.service.RefreshTokenService;
import com.cms.util.HashUtil;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final String INVALID_CODE = "INVALID_REFRESH_TOKEN";
    private static final String INVALID_MESSAGE = "Your session is no longer valid. Please sign in again.";
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.refreshTtl = Duration.ofDays(properties.refreshTtlDays());
    }

    @Override
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(refreshTtl);
        repository.save(new RefreshToken(userId, HashUtil.sha256Hex(rawToken), expiresAt));
        return rawToken;
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Long consume(String rawToken) {
        RefreshToken token = repository.findByTokenHash(HashUtil.sha256Hex(rawToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CODE, INVALID_MESSAGE));

        if (token.isRevoked()) {
            int revoked = repository.revokeAllByUserId(token.getUserId());
            log.warn("Revoked refresh token was reused; all sessions revoked: userId={} revokedTokens={}",
                    token.getUserId(), revoked);
            throw new UnauthorizedException(INVALID_CODE, INVALID_MESSAGE);
        }
        if (token.isExpiredAt(clock.instant())) {
            log.info("Expired refresh token presented: userId={}", token.getUserId());
            throw new UnauthorizedException(INVALID_CODE, INVALID_MESSAGE);
        }
        token.setRevoked(true);
        repository.save(token);
        return token.getUserId();
    }

    @Override
    @Transactional
    public Optional<Long> revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHash(HashUtil.sha256Hex(rawToken))
                .filter(token -> !token.isRevoked())
                .map(token -> {
                    token.setRevoked(true);
                    repository.save(token);
                    return token.getUserId();
                });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId);
    }
}
