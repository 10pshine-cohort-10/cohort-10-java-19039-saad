package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.dto.request.LoginRequest;
import com.cms.dto.request.RegisterRequest;
import com.cms.dto.response.AuthResponse;
import com.cms.dto.response.FieldErrorDetail;
import com.cms.entity.User;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.InvalidCredentialsException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.UnauthorizedException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.security.JwtService;
import com.cms.service.impl.AuthServiceImpl;
import com.cms.support.TestData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, refreshTokenService,
                new UserMapper());
    }

    private void stubTokenIssuing(long userId) {
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(userId)).thenReturn("refresh-token");
    }

    // ---------- registration ----------

    @Test
    void register_normalizesInputHashesPasswordAndIssuesTokens() {
        RegisterRequest request = TestData.registerRequest(" Jane.Doe@Example.COM ", "+1 (555) 123-4567");
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+15551234567")).thenReturn(false);
        when(passwordEncoder.encode(TestData.VALID_PASSWORD)).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        stubTokenIssuing(1L);

        AuthResponse response = service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(saved.getPhone()).isEqualTo("+15551234567");
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password").isNotEqualTo(TestData.VALID_PASSWORD);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void register_withExistingEmail_isRejectedWithEmailFieldError() {
        RegisterRequest request = TestData.registerRequest("jane@example.com", "+15551234567");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);
        when(userRepository.existsByPhone("+15551234567")).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOfSatisfying(DuplicateResourceException.class, ex ->
                        assertThat(ex.getFieldErrors()).extracting(FieldErrorDetail::field).containsExactly("email"));

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(TestData.VALID_PASSWORD);
    }

    @Test
    void register_withExistingPhone_isRejectedWithPhoneFieldError() {
        RegisterRequest request = TestData.registerRequest("jane@example.com", "+15551234567");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+15551234567")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOfSatisfying(DuplicateResourceException.class, ex ->
                        assertThat(ex.getFieldErrors()).extracting(FieldErrorDetail::field).containsExactly("phone"));
    }

    @Test
    void register_withExistingEmailAndPhone_reportsBothFields() {
        RegisterRequest request = TestData.registerRequest("jane@example.com", "+15551234567");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);
        when(userRepository.existsByPhone("+15551234567")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOfSatisfying(DuplicateResourceException.class, ex ->
                        assertThat(ex.getFieldErrors()).extracting(FieldErrorDetail::field)
                                .containsExactlyInAnyOrder("email", "phone"));
    }

    @Test
    void register_whenConcurrentInsertHitsUniqueConstraint_isReportedAsDuplicate() {
        RegisterRequest request = TestData.registerRequest("jane@example.com", "+15551234567");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+15551234567")).thenReturn(false);
        when(passwordEncoder.encode(TestData.VALID_PASSWORD)).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("UQ_users_email"));

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(DuplicateResourceException.class);
        verify(refreshTokenService, never()).issue(any());
    }

    // ---------- login ----------

    @Test
    void login_withEmail_isCaseInsensitiveAndIssuesTokens() {
        User user = TestData.user(5L, "jane@example.com", "+15551234567", "stored-hash");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        stubTokenIssuing(5L);

        AuthResponse response = service.login(new LoginRequest("  Jane@Example.com ", "secret"));

        assertThat(response.user().id()).isEqualTo(5L);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, never()).findByPhone(any());
    }

    @Test
    void login_withFormattedPhone_looksUpNormalizedNumber() {
        User user = TestData.user(6L, "jane@example.com", "+15551234567", "stored-hash");
        when(userRepository.findByPhone("+15551234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        stubTokenIssuing(6L);

        AuthResponse response = service.login(new LoginRequest("+1 (555) 123-4567", "secret"));

        assertThat(response.user().id()).isEqualTo(6L);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsAndIssuesNoTokens() {
        User user = TestData.user(5L, "jane@example.com", "+15551234567", "stored-hash");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("jane@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenService, never()).issue(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_withUnknownUser_stillChecksAPasswordHashAndFailsWithSameError() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email/phone or password.");

        // A BCrypt comparison is performed anyway so timing does not reveal whether the account exists.
        verify(passwordEncoder).matches(eq("whatever"), any());
        verify(refreshTokenService, never()).issue(any());
    }

    // ---------- refresh / logout / current user ----------

    @Test
    void refresh_rotatesTokenForTheOwningUser() {
        User user = TestData.user(9L, "jane@example.com", "+15551234567", "hash");
        when(refreshTokenService.consume("old-refresh")).thenReturn(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        stubTokenIssuing(9L);

        AuthResponse response = service.refresh("old-refresh");

        assertThat(response.user().id()).isEqualTo(9L);
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refresh_withInvalidToken_propagatesUnauthorizedWithoutLoadingUser() {
        when(refreshTokenService.consume("bad"))
                .thenThrow(new UnauthorizedException("INVALID_REFRESH_TOKEN", "invalid"));

        assertThatThrownBy(() -> service.refresh("bad")).isInstanceOf(UnauthorizedException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refresh_whenUserWasDeleted_isUnauthorized() {
        when(refreshTokenService.consume("orphan")).thenReturn(77L);
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("orphan")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_revokesTheSuppliedRefreshToken() {
        when(refreshTokenService.revoke("my-refresh")).thenReturn(Optional.of(3L));

        service.logout("my-refresh");

        verify(refreshTokenService).revoke("my-refresh");
    }

    @Test
    void getCurrentUser_returnsMappedUser() {
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(TestData.user(5L, "jane@example.com", "+15551234567", "hash")));

        assertThat(service.getCurrentUser(5L).email()).isEqualTo("jane@example.com");
    }

    @Test
    void getCurrentUser_whenMissing_throwsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
