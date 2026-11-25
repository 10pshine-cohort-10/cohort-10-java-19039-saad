package com.cms.service.impl;

import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.exception.FileProcessingException;
import com.cms.repository.ContactRepository;
import com.cms.service.CsvExportService;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvExportServiceImpl implements CsvExportService {

    private static final Logger log = LoggerFactory.getLogger(CsvExportServiceImpl.class);

    private static final String[] HEADERS = {
            "First Name", "Last Name", "Title", "Company", "Job Position",
            "Primary Email", "Secondary Emails", "Primary Phone", "Secondary Phones",
            "Address", "City", "State", "Country", "Postal Code", "Birthday", "Notes"
    };

    private final ContactRepository contactRepository;

    public CsvExportServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportContactsToCsv(Long userId) {
        log.info("Exporting contacts to CSV for userId={}", userId);

        Specification<Contact> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        List<Contact> contacts = contactRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "lastName", "firstName"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Write UTF-8 BOM for Microsoft Excel compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {

                for (Contact c : contacts) {
                    String primaryEmail = c.getEmails().stream()
                            .filter(ContactEmail::isPrimary)
                            .map(ContactEmail::getEmail)
                            .findFirst()
                            .orElse(c.getEmails().isEmpty() ? "" : c.getEmails().get(0).getEmail());

                    String secondaryEmails = c.getEmails().stream()
                            .filter(e -> !e.isPrimary())
                            .map(ContactEmail::getEmail)
                            .collect(Collectors.joining("; "));

                    String primaryPhone = c.getPhoneNumbers().stream()
                            .filter(ContactPhoneNumber::isPrimary)
                            .map(ContactPhoneNumber::getPhoneNumber)
                            .findFirst()
                            .orElse(c.getPhoneNumbers().isEmpty() ? "" : c.getPhoneNumbers().get(0).getPhoneNumber());

                    String secondaryPhones = c.getPhoneNumbers().stream()
                            .filter(p -> !p.isPrimary())
                            .map(ContactPhoneNumber::getPhoneNumber)
                            .collect(Collectors.joining("; "));

                    printer.printRecord(
                            sanitize(c.getFirstName()),
                            sanitize(c.getLastName()),
                            sanitize(c.getTitle()),
                            sanitize(c.getCompany()),
                            sanitize(c.getJobPosition()),
                            sanitize(primaryEmail),
                            sanitize(secondaryEmails),
                            sanitize(primaryPhone),
                            sanitize(secondaryPhones),
                            sanitize(c.getAddress()),
                            sanitize(c.getCity()),
                            sanitize(c.getState()),
                            sanitize(c.getCountry()),
                            sanitize(c.getPostalCode()),
                            c.getBirthday() != null ? c.getBirthday().toString() : "",
                            sanitize(c.getNotes())
                    );
                }
                printer.flush();
            }

            log.info("Exported {} contacts to CSV for userId={}", contacts.size(), userId);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate contacts CSV export for userId={}", userId, e);
            throw new FileProcessingException("Could not generate CSV export: " + e.getMessage());
        }
    }

    @Override
    public byte[] getCsvTemplate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {

                // Sample row
                printer.printRecord(
                        "Jane", "Doe", "VP of Engineering", "Acme Tech", "Software Architect",
                        "jane.doe@example.com", "jane.personal@gmail.com", "+1 555 123 4567", "+1 555 987 6543",
                        "100 Innovation Way", "San Francisco", "CA", "USA", "94105", "1990-05-15",
                        "Key technical partner for cloud infrastructure"
                );
                printer.flush();
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new FileProcessingException("Could not generate CSV template: " + e.getMessage());
        }
    }

    /**
     * Prevents CSV Formula Injection in spreadsheet tools (Excel, LibreOffice).
     * Cells starting with =, +, -, or @ are prepended with an apostrophe.
     */
    private String sanitize(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed;
        }
        return trimmed;
    }
}
