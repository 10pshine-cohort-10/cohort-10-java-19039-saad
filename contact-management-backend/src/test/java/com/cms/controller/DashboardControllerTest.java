package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.config.CorsConfig;
import com.cms.config.SecurityConfig;
import com.cms.dto.response.DashboardSummaryResponse;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.security.AccessTokenClaims;
import com.cms.security.JwtService;
import com.cms.service.DashboardService;
import com.cms.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void summary_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void summary_returnsSummaryWhenAuthenticated() throws Exception {
        String token = "valid.access.token";
        User user = TestData.user(100L, "jane@example.com", "+15551234567", "$2a$12$hash");
        when(jwtService.parseAccessToken(token))
                .thenReturn(new AccessTokenClaims(user.getId(), user.getTokenVersion()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        DashboardSummaryResponse response = new DashboardSummaryResponse(15, 5, 2, List.of());
        when(dashboardService.getSummary(user.getId())).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContacts").value(15))
                .andExpect(jsonPath("$.contactsThisMonth").value(5))
                .andExpect(jsonPath("$.contactsThisWeek").value(2))
                .andExpect(jsonPath("$.recentContacts").isArray());

        verify(dashboardService).getSummary(user.getId());
    }
}
