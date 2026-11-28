package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.config.CorsConfig;
import com.cms.config.SecurityConfig;
import com.cms.dto.request.ContactEmailRequest;
import com.cms.dto.request.ContactPhoneRequest;
import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.dto.response.PageResponse;
import com.cms.entity.EmailLabel;
import com.cms.entity.PhoneLabel;
import com.cms.entity.User;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.UserRepository;
import com.cms.security.AccessTokenClaims;
import com.cms.security.JwtService;
import com.cms.service.ContactService;
import com.cms.support.TestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContactController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@ActiveProfiles("test")
class ContactControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ContactService contactService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserRepository userRepository;

        private final String token = "mock.jwt.token";
        private final User user = TestData.user(100L, "john@example.com", "+15551234567", "hash");

        @BeforeEach
        void setUp() {
                when(jwtService.parseAccessToken(token))
                                .thenReturn(new AccessTokenClaims(user.getId(), user.getTokenVersion()));
                when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        }

        @Test
        void getContacts_requiresAuthentication() throws Exception {
                mockMvc.perform(get("/api/v1/contacts"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void getContacts_returnsPaginatedList() throws Exception {
                ContactSummaryResponse summary = new ContactSummaryResponse(
                                1L, "Bruce", "Wayne", "CEO", "Wayne Enterprises", "bruce@wayne.com", "+15551234567",
                                Instant.now(), Instant.now());
                PageResponse<ContactSummaryResponse> page = new PageResponse<>(List.of(summary), 0, 10, 1, 1, true,
                                true);

                when(contactService.getContacts(eq(user.getId()), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/contacts")
                                .header("Authorization", "Bearer " + token)
                                .param("query", "Wayne")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].firstName").value("Bruce"))
                                .andExpect(jsonPath("$.content[0].company").value("Wayne Enterprises"));
        }

        @Test
        void createContact_withValidPayload_returnsCreated() throws Exception {
                CreateContactRequest request = new CreateContactRequest(
                                "Diana", "Prince", "Ambassador", "Themyscira Embassy", "Diplomat", null,
                                null, null, null, null, null, null, null, null,
                                List.of(new ContactEmailRequest("diana@embassy.org", EmailLabel.WORK, true)),
                                List.of(new ContactPhoneRequest("+15550001122", PhoneLabel.WORK, true)));

                ContactDetailResponse detail = new ContactDetailResponse(
                                5L, "Diana", "Prince", "Ambassador", "Themyscira Embassy", "Diplomat",
                                null, null, null, null, null, null, null, null, null,
                                List.of(), List.of(), Instant.now(), Instant.now());

                when(contactService.createContact(eq(user.getId()), any(CreateContactRequest.class)))
                                .thenReturn(detail);

                mockMvc.perform(post("/api/v1/contacts")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(5))
                                .andExpect(jsonPath("$.firstName").value("Diana"));
        }

        @Test
        void createContact_withBlankFirstName_returnsBadRequest() throws Exception {
                CreateContactRequest request = new CreateContactRequest(
                                "", "Prince", null, null, null, null,
                                null, null, null, null, null, null, null, null,
                                List.of(), List.of());

                mockMvc.perform(post("/api/v1/contacts")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));
        }

        @Test
        void deleteContact_returnsNoContent() throws Exception {
                mockMvc.perform(delete("/api/v1/contacts/5")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNoContent());

                verify(contactService).deleteContact(5L, user.getId());
        }

        @Test
        void getContactById_whenNotFound_returns404() throws Exception {
                when(contactService.getContactById(999L, user.getId()))
                                .thenThrow(new ResourceNotFoundException("Contact not found with id: 999"));

                mockMvc.perform(get("/api/v1/contacts/999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
}
