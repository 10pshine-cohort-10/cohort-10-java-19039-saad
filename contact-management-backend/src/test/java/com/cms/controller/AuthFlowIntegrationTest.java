package com.cms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.support.AbstractSqlServerTest;
import com.cms.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Full-stack flow (controller, security, services, JPA, Flyway) against a real SQL Server container. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest extends AbstractSqlServerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private record Account(String email, String phone) {
    }

    private static Account newAccount() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String phone = "+1555" + (1_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));
        return new Account(email, phone);
    }

    private MvcResult postJson(String url, Object body, int expectedStatus) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private JsonNode register(Account account) throws Exception {
        Map<String, String> body = Map.of("firstName", "Jane", "lastName", "Doe", "email", account.email(),
                "phone", account.phone(), "password", TestData.VALID_PASSWORD,
                "confirmPassword", TestData.VALID_PASSWORD);
        return objectMapper.readTree(postJson("/api/v1/auth/register", body, 201).getResponse().getContentAsString());
    }

    @Test
    void registerThenLoginWithEmailAndPhone_thenAccessProfile() throws Exception {
        Account account = newAccount();
        JsonNode registered = register(account);
        assertThat(registered.get("accessToken").asText()).isNotBlank();

        postJson("/api/v1/auth/login", Map.of("identifier", account.email().toUpperCase(),
                "password", TestData.VALID_PASSWORD), 200);
        MvcResult phoneLogin = postJson("/api/v1/auth/login",
                Map.of("identifier", account.phone(), "password", TestData.VALID_PASSWORD), 200);
        String token = objectMapper.readTree(phoneLogin.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void duplicateRegistration_returns409() throws Exception {
        Account account = newAccount();
        register(account);

        Map<String, String> body = Map.of("firstName", "Other", "lastName", "Person", "email", account.email(),
                "phone", newAccount().phone(), "password", TestData.VALID_PASSWORD,
                "confirmPassword", TestData.VALID_PASSWORD);
        MvcResult result = postJson("/api/v1/auth/register", body, 409);
        assertThat(result.getResponse().getContentAsString()).contains("DUPLICATE_RESOURCE").contains("email");
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        Account account = newAccount();
        register(account);

        postJson("/api/v1/auth/login", Map.of("identifier", account.email(), "password", "Wrong!Pass123"), 401);
    }

    @Test
    void refreshTokensAreSingleUse_andReuseRevokesTheWholeFamily() throws Exception {
        JsonNode first = register(newAccount());
        String firstRefresh = first.get("refreshToken").asText();

        MvcResult rotated = postJson("/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh), 200);
        String secondRefresh = objectMapper.readTree(rotated.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Replaying the first (already used) token is rejected...
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh), 401);
        // ...and counts as theft: the newer token is revoked too.
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", secondRefresh), 401);
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        JsonNode session = register(newAccount());
        String refresh = session.get("refreshToken").asText();

        postJson("/api/v1/auth/logout", Map.of("refreshToken", refresh), 204);
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", refresh), 401);
    }

    @Test
    void protectedEndpoint_rejectsMissingAndForgedTokens() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }
}
