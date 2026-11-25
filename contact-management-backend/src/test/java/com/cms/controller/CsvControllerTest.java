package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.config.CorsConfig;
import com.cms.config.SecurityConfig;
import com.cms.dto.response.CsvImportSummaryResponse;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.security.AccessTokenClaims;
import com.cms.security.JwtService;
import com.cms.service.CsvExportService;
import com.cms.service.CsvImportService;
import com.cms.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CsvController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@ActiveProfiles("test")
class CsvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CsvExportService csvExportService;

    @MockitoBean
    private CsvImportService csvImportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private final String token = "csv.access.token";
    private final User user = TestData.user(30L, "csv@example.com", "+15553334444", "hash");

    @BeforeEach
    void setUp() {
        when(jwtService.parseAccessToken(token))
                .thenReturn(new AccessTokenClaims(user.getId(), user.getTokenVersion()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void exportContacts_returnsCsvAttachment() throws Exception {
        byte[] csvData = "First Name,Last Name\nArthur,Dent\n".getBytes(StandardCharsets.UTF_8);
        when(csvExportService.exportContactsToCsv(user.getId())).thenReturn(csvData);

        mockMvc.perform(get("/api/v1/csv/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"contacts.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }

    @Test
    void downloadTemplate_returnsCsvTemplate() throws Exception {
        byte[] templateData = "First Name,Last Name\n".getBytes(StandardCharsets.UTF_8);
        when(csvExportService.getCsvTemplate()).thenReturn(templateData);

        mockMvc.perform(get("/api/v1/csv/template")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"contacts_template.csv\""));
    }

    @Test
    void importContacts_returnsSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        CsvImportSummaryResponse summary = new CsvImportSummaryResponse(5, 4, 1, 0, List.of());

        when(csvImportService.importContactsFromCsv(eq(user.getId()), any()))
                .thenReturn(summary);

        mockMvc.perform(multipart("/api/v1/csv/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(5))
                .andExpect(jsonPath("$.importedRows").value(4))
                .andExpect(jsonPath("$.duplicateRows").value(1));
    }
}
