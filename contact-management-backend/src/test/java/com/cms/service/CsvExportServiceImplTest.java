package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.entity.EmailLabel;
import com.cms.entity.PhoneLabel;
import com.cms.entity.User;
import com.cms.repository.ContactRepository;
import com.cms.service.impl.CsvExportServiceImpl;
import com.cms.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    private CsvExportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CsvExportServiceImpl(contactRepository);
    }

    @Test
    void exportContactsToCsv_generatesUtf8CsvWithHeadersAndSanitizesFormulas() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");

        Contact contact = new Contact();
        contact.setUser(user);
        contact.setFirstName("=cmd"); // Formula injection test
        contact.setLastName("Danger");
        contact.setCompany("Secure Corp");
        contact.addEmail(new ContactEmail("danger@secure.com", EmailLabel.WORK, true));
        contact.addPhoneNumber(new ContactPhoneNumber("+15550000000", PhoneLabel.WORK, true));

        when(contactRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(contact));

        byte[] result = service.exportContactsToCsv(userId);

        assertThat(result).isNotEmpty();
        String content = new String(result, StandardCharsets.UTF_8);

        // Assert Excel BOM exists at the start
        assertThat(result[0]).isEqualTo((byte) 0xEF);
        assertThat(result[1]).isEqualTo((byte) 0xBB);
        assertThat(result[2]).isEqualTo((byte) 0xBF);

        // Assert formula injection is escaped with leading single quote
        assertThat(content).contains("'=cmd");
        assertThat(content).contains("Danger");
        assertThat(content).contains("danger@secure.com");
    }

    @Test
    void getCsvTemplate_returnsHeadersAndSampleRow() {
        byte[] template = service.getCsvTemplate();

        assertThat(template).isNotEmpty();
        String content = new String(template, StandardCharsets.UTF_8);
        assertThat(content).contains("First Name");
        assertThat(content).contains("Last Name");
        assertThat(content).contains("Primary Email");
    }
}
