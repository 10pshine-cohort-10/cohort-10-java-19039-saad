package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.config.CorsConfig;
import com.cms.config.SecurityConfig;
import com.cms.dto.request.RegisterRequest;
import com.cms.dto.response.AuthResponse;
import com.cms.dto.response.FieldErrorDetail;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.InvalidCredentialsException;
import com.cms.exception.UnauthorizedException;
import com.cms.repository.UserRepository;
import com.cms.security.AccessTokenClaims;
import com.cms.security.JwtService;
import com.cms.service.AuthService;
import com.cms.support.TestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserRepository userRepository;

    private static UserResponse userResponse() {
        return new UserResponse(7L, "Jane", "Doe", "jane@example.com", "+15551234567",
                Instant.parse("2025-01-01T00:00:00Z"));
    }

    private static AuthResponse authResponse() {
        return AuthResponse.bearer("access-token", "refresh-token", 900, userResponse());
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, String> validRegistration() {
        return Map.of("firstName", "Jane", "lastName", "Doe", "email", "jane@example.com",
                "phone", "+1 555 123 4567", "password", TestData.VALID_PASSWORD,
                "confirmPassword", TestData.VALID_PASSWORD);
    }

    // ---------- register ----------

    @Test
    void register_withValidBody_returns201AndTokens() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegistration())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void register_withWeakPasswordMismatchAndBadPhone_returns400WithFieldErrors() throws Exception {
        Map<String, String> body = Map.of("firstName", "Jane", "lastName", "Doe", "email", "jane@example.com",
                "phone", "abc", "password", "weak", "confirmPassword", "other");

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='phone')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='confirmPassword')]").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_withMissingBody_returns400Malformed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void register_withDuplicateEmail_returns409WithFieldError() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new DuplicateResourceException(
                "An account with these details already exists.",
                List.of(new FieldErrorDetail("email", "This email address is already registered"))));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegistration())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    // ---------- login ----------

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        when(authService.login(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", "jane@example.com", "password", "x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_withBlankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", " ", "password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='identifier')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists());
    }

    @Test
    void login_withInvalidCredentials_returns401WithoutRevealingWhichFieldWasWrong() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", "jane@example.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email/phone or password."));
    }

    // ---------- refresh / logout ----------

    @Test
    void refresh_withValidToken_returnsNewTokens() throws Exception {
        when(authService.refresh("good-refresh")).thenReturn(authResponse());

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "good-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refresh_withRevokedToken_returns401() throws Exception {
        when(authService.refresh("revoked")).thenThrow(
                new UnauthorizedException("INVALID_REFRESH_TOKEN", "Your session is no longer valid."));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "revoked"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logout_revokesTokenAndReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "my-refresh"))))
                .andExpect(status().isNoContent());

        verify(authService).logout("my-refresh");
    }

    @Test
    void logout_withoutBody_stillReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService).logout(null);
    }

    // ---------- protected endpoints ----------

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }

    @Test
    void unknownPrivateRoute_withoutToken_returns401NotAnInformationLeakingStatus() throws Exception {
        mockMvc.perform(get("/api/v1/contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withExpiredToken_returns401TokenExpired() throws Exception {
        when(jwtService.parseAccessToken("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void me_withMalformedToken_returns401TokenInvalid() throws Exception {
        when(jwtService.parseAccessToken("garbage")).thenThrow(new MalformedJwtException("bad"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void me_withTokenFromBeforePasswordChange_returns401() throws Exception {
        User current = TestData.user(7L, "jane@example.com", "+15551234567", "hash");
        current.setTokenVersion(1);
        when(jwtService.parseAccessToken("stale")).thenReturn(new AccessTokenClaims(7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(current));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer stale"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void me_withValidToken_returnsTheAuthenticatedUsersProfile() throws Exception {
        User current = TestData.user(7L, "jane@example.com", "+15551234567", "hash");
        when(jwtService.parseAccessToken("valid")).thenReturn(new AccessTokenClaims(7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(current));
        when(authService.getCurrentUser(7L)).thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void methodNotAllowed_forAuthenticatedCaller_returnsConsistentErrorBody() throws Exception {
        User current = TestData.user(7L, "jane@example.com", "+15551234567", "hash");
        when(jwtService.parseAccessToken("valid")).thenReturn(new AccessTokenClaims(7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(current));

        mockMvc.perform(post("/api/v1/auth/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
