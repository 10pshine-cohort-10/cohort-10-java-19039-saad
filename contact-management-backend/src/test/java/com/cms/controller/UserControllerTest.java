package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.config.CorsConfig;
import com.cms.config.SecurityConfig;
import com.cms.dto.request.ChangePasswordRequest;
import com.cms.dto.request.UpdateProfileRequest;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.security.AccessTokenClaims;
import com.cms.security.JwtService;
import com.cms.service.UserService;
import com.cms.support.TestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private final String token = "user.access.token";
    private final User user = TestData.user(20L, "profile@example.com", "+15551112222", "hash");

    @BeforeEach
    void setUp() {
        when(jwtService.parseAccessToken(token))
                .thenReturn(new AccessTokenClaims(user.getId(), user.getTokenVersion()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void getProfile_returnsAuthenticatedUserProfile() throws Exception {
        UserResponse response = new UserResponse(20L, "Jane", "Doe", "profile@example.com", "+15551112222", Instant.now());
        when(userService.getProfile(user.getId())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void updateProfile_success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Updated", "+15551112222");
        UserResponse response = new UserResponse(20L, "Jane", "Updated", "profile@example.com", "+15551112222", Instant.now());
        when(userService.updateProfile(eq(user.getId()), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Updated"));
    }

    @Test
    void changePassword_success_returnsNoContent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass!123", "NewStr0ng!Pass1", "NewStr0ng!Pass1");

        mockMvc.perform(post("/api/v1/users/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(user.getId()), any(ChangePasswordRequest.class));
    }
}
