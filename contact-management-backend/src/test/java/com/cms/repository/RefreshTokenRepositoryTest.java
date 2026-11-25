package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cms.entity.RefreshToken;
import com.cms.entity.User;
import com.cms.support.AbstractSqlServerTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefreshTokenRepositoryTest extends AbstractSqlServerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User savedUser(String email, String phone) {
        User user = new User();
        user.setFirstName("Rita");
        user.setLastName("Token");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("$2a$12$hash");
        return userRepository.saveAndFlush(user);
    }

    private RefreshToken savedToken(Long userId, String hash) {
        return refreshTokenRepository.saveAndFlush(
                new RefreshToken(userId, hash, Instant.now().plus(7, ChronoUnit.DAYS)));
    }

    @Test
    void findByTokenHash_returnsStoredToken() {
        User user = savedUser("rt1@example.com", "+20000000001");
        savedToken(user.getId(), "a".repeat(64));

        assertThat(refreshTokenRepository.findByTokenHash("a".repeat(64)))
                .hasValueSatisfying(token -> {
                    assertThat(token.getUserId()).isEqualTo(user.getId());
                    assertThat(token.isRevoked()).isFalse();
                    assertThat(token.getCreatedAt()).isNotNull();
                });
        assertThat(refreshTokenRepository.findByTokenHash("b".repeat(64))).isEmpty();
    }

    @Test
    void revokeAllByUserId_revokesOnlyThatUsersLiveTokens() {
        User owner = savedUser("rt2@example.com", "+20000000002");
        User other = savedUser("rt3@example.com", "+20000000003");
        savedToken(owner.getId(), "c".repeat(64));
        savedToken(owner.getId(), "d".repeat(64));
        savedToken(other.getId(), "e".repeat(64));

        int revoked = refreshTokenRepository.revokeAllByUserId(owner.getId());

        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenHash("c".repeat(64)).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByTokenHash("d".repeat(64)).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByTokenHash("e".repeat(64)).orElseThrow().isRevoked()).isFalse();
    }
}
