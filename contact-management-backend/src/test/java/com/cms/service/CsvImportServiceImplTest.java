package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.dto.response.CsvImportSummaryResponse;
import com.cms.entity.Contact;
import com.cms.entity.User;
import com.cms.exception.InvalidCsvFormatException;
import com.cms.repository.ContactRepository;
import com.cms.repository.UserRepository;
import com.cms.service.impl.CsvImportServiceImpl;
import com.cms.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    private CsvImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CsvImportServiceImpl(contactRepository, userRepository);
    }

    @Test
    void importContactsFromCsv_withValidData_importsSuccessfully() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contactRepository.findAll(any(Specification.class))).thenReturn(List.of());

        String csvContent = "First Name,Last Name,Company,Primary Email,Primary Phone\n" +
                "Clark,Kent,Daily Planet,clark@dailyplanet.com,+15551112233\n" +
                "Lois,Lane,Daily Planet,lois@dailyplanet.com,+15552223344\n";

        MockMultipartFile file = new MockMultipartFile("file", "contacts.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportSummaryResponse summary = service.importContactsFromCsv(userId, file);

        assertThat(summary.totalRows()).isEqualTo(2);
        assertThat(summary.importedRows()).isEqualTo(2);
        assertThat(summary.duplicateRows()).isZero();
        assertThat(summary.failedRows()).isZero();
        verify(contactRepository).saveAll(anyList());
    }

    @Test
    void importContactsFromCsv_withExcelStyleHeaders_importsSuccessfully() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contactRepository.findAll(any(Specification.class))).thenReturn(List.of());

        String csvContent = "\uFEFFFirst Name, Last Name , Company , Primary Email , Primary Phone\n" +
                "Clark, Kent, Daily Planet, clark@dailyplanet.com , +15551112233 \n" +
                "Lois, Lane, Daily Planet, lois@dailyplanet.com, +15552223344\n";

        MockMultipartFile file = new MockMultipartFile("file", "contacts.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportSummaryResponse summary = service.importContactsFromCsv(userId, file);

        assertThat(summary.totalRows()).isEqualTo(2);
        assertThat(summary.importedRows()).isEqualTo(2);
        assertThat(summary.failedRows()).isZero();
    }

    @Test
    void importContactsFromCsv_withEmptyFile_throwsInvalidCsvFormatException() {
        Long userId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.importContactsFromCsv(userId, file))
                .isInstanceOf(InvalidCsvFormatException.class);
    }

    @Test
    void importContactsFromCsv_withInvalidRow_recordsErrorAndImportsValid() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contactRepository.findAll(any(Specification.class))).thenReturn(List.of());

        String csvContent = "First Name,Last Name,Company,Primary Email,Primary Phone\n" +
                ",MissingFirst,Acme,valid@acme.com,+15551112233\n" +
                "Valid,Person,Acme,valid2@acme.com,+15552223344\n";

        MockMultipartFile file = new MockMultipartFile("file", "contacts.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportSummaryResponse summary = service.importContactsFromCsv(userId, file);

        assertThat(summary.totalRows()).isEqualTo(2);
        assertThat(summary.importedRows()).isEqualTo(1);
        assertThat(summary.failedRows()).isEqualTo(1);
        assertThat(summary.errors()).isNotEmpty();
    }
}
