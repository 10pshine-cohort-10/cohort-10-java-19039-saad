package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cms.config.JwtProperties;
import com.cms.entity.RefreshToken;
import com.cms.exception.UnauthorizedException;
import com.cms.repository.RefreshTokenRepository;
import com.cms.service.impl.RefreshTokenServiceImpl;
import com.cms.util.HashUtil;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2025-03-01T12:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(repository, new JwtProperties("unused", 15, 7),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issue_storesOnlyTheHashAndSetsSevenDayExpiry() {
        String raw = service.issue(10L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getTokenHash()).isEqualTo(HashUtil.sha256Hex(raw)).isNotEqualTo(raw);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(saved.isRevoked()).isFalse();
        assertThat(raw).hasSizeGreaterThanOrEqualTo(64);
    }

    @Test
    void issue_producesDifferentTokensEachTime() {
        assertThat(service.issue(1L)).isNotEqualTo(service.issue(1L));
    }

    @Test
    void consume_validToken_revokesItAndReturnsOwner() {
        RefreshToken token = new RefreshToken(9L, HashUtil.sha256Hex("raw"), NOW.plusSeconds(60));
        when(repository.findByTokenHash(HashUtil.sha256Hex("raw"))).thenReturn(Optional.of(token));

        Long userId = service.consume("raw");

        assertThat(userId).isEqualTo(9L);
        assertThat(token.isRevoked()).isTrue();
        verify(repository).save(token);
    }

    @Test
    void consume_unknownToken_isUnauthorized() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("nope"))
                .isInstanceOfSatisfying(UnauthorizedException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void consume_reusedRevokedToken_revokesEveryOtherSessionOfThatUser() {
        RefreshToken token = new RefreshToken(9L, HashUtil.sha256Hex("stolen"), NOW.plusSeconds(60));
        token.setRevoked(true);
        when(repository.findByTokenHash(HashUtil.sha256Hex("stolen"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("stolen")).isInstanceOf(UnauthorizedException.class);

        verify(repository).revokeAllByUserId(9L);
    }

    @Test
    void consume_expiredToken_isUnauthorizedAndNotRotated() {
        RefreshToken token = new RefreshToken(9L, HashUtil.sha256Hex("old"), NOW.minusSeconds(1));
        when(repository.findByTokenHash(HashUtil.sha256Hex("old"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("old")).isInstanceOf(UnauthorizedException.class);

        verify(repository, never()).revokeAllByUserId(any());
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revoke_liveToken_marksItRevokedAndReturnsOwner() {
        RefreshToken token = new RefreshToken(4L, HashUtil.sha256Hex("live"), NOW.plusSeconds(60));
        when(repository.findByTokenHash(HashUtil.sha256Hex("live"))).thenReturn(Optional.of(token));

        assertThat(service.revoke("live")).contains(4L);
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void revoke_blankToken_doesNothing() {
        assertThat(service.revoke("  ")).isEmpty();
        assertThat(service.revoke(null)).isEmpty();
        verifyNoInteractions(repository);
    }
}
