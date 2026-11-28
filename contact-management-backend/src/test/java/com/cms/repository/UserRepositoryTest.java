package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cms.entity.User;
import com.cms.support.AbstractSqlServerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Runs against a real SQL Server (Testcontainers) so Flyway migrations and constraints are exercised. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest extends AbstractSqlServerTest {

    @Autowired
    private UserRepository userRepository;

    private static User newUser(String email, String phone) {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("$2a$12$hash");
        return user;
    }

    @Test
    void save_assignsIdAndTimestamps() {
        User saved = userRepository.saveAndFlush(newUser("a@example.com", "+10000000001"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getTokenVersion()).isZero();
    }

    @Test
    void findByEmailAndPhone_returnTheMatchingUser() {
        userRepository.saveAndFlush(newUser("b@example.com", "+10000000002"));

        assertThat(userRepository.findByEmail("b@example.com")).isPresent();
        assertThat(userRepository.findByPhone("+10000000002")).isPresent();
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("b@example.com")).isTrue();
        assertThat(userRepository.existsByPhone("+19999999999")).isFalse();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        userRepository.saveAndFlush(newUser("c@example.com", "+10000000003"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("c@example.com", "+10000000004")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePhone_violatesUniqueConstraint() {
        userRepository.saveAndFlush(newUser("d@example.com", "+10000000005"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("e@example.com", "+10000000005")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unicodeNames_surviveARoundTrip() {
        User user = newUser("f@example.com", "+10000000006");
        user.setFirstName("Zoë");
        user.setLastName("Müller-Åström 李");
        userRepository.saveAndFlush(user);

        User loaded = userRepository.findByEmail("f@example.com").orElseThrow();
        assertThat(loaded.getFirstName()).isEqualTo("Zoë");
        assertThat(loaded.getLastName()).isEqualTo("Müller-Åström 李");
    }
}
