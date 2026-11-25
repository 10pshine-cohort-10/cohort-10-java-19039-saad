package com.cms.service.impl;

import com.cms.dto.response.CsvImportSummaryResponse;
import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.entity.EmailLabel;
import com.cms.entity.PhoneLabel;
import com.cms.entity.User;
import com.cms.exception.FileProcessingException;
import com.cms.exception.InvalidCsvFormatException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.ContactRepository;
import com.cms.repository.UserRepository;
import com.cms.service.CsvImportService;
import com.cms.util.EmailNormalizer;
import com.cms.util.PhoneNormalizer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportServiceImpl implements CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportServiceImpl.class);
    private static final int MAX_ROWS = 5000;

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public CsvImportServiceImpl(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CsvImportSummaryResponse importContactsFromCsv(Long userId, MultipartFile file) {
        log.info("Starting CSV import for userId={}, fileName={}, size={}", userId, file.getOriginalFilename(),
                file.getSize());

        if (file.isEmpty()) {
            throw new InvalidCsvFormatException("Uploaded CSV file is empty.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".csv")) {
            throw new InvalidCsvFormatException("Invalid file type. Please upload a standard CSV (.csv) file.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Track existing contacts for duplicate detection
        List<Contact> existingContacts = contactRepository
                .findAll((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        Set<String> existingEmails = new HashSet<>();
        Set<String> existingNameCompany = new HashSet<>();

        for (Contact c : existingContacts) {
            c.getEmails().forEach(e -> existingEmails.add(e.getEmail().toLowerCase()));
            String key = (c.getFirstName() + "|" + c.getLastName() + "|"
                    + (c.getCompany() != null ? c.getCompany() : "")).toLowerCase();
            existingNameCompany.add(key);
        }

        int totalRows = 0;
        int importedRows = 0;
        int duplicateRows = 0;
        List<CsvImportSummaryResponse.RowErrorDetail> errors = new ArrayList<>();
        List<Contact> contactsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)) {

            for (CSVRecord record : parser) {
                totalRows++;
                if (totalRows > MAX_ROWS) {
                    errors.add(new CsvImportSummaryResponse.RowErrorDetail(totalRows,
                            "File exceeds maximum limit of " + MAX_ROWS + " rows."));
                    break;
                }

                int rowNumber = (int) record.getRecordNumber() + 1; // 1-based line including header

                try {
                    String firstName = getColumn(record, "First Name", "FirstName", "first_name");
                    String lastName = getColumn(record, "Last Name", "LastName", "last_name");

                    if (firstName.isBlank()) {
                        errors.add(new CsvImportSummaryResponse.RowErrorDetail(rowNumber, "First Name is required."));
                        continue;
                    }
                    if (lastName.isBlank()) {
                        errors.add(new CsvImportSummaryResponse.RowErrorDetail(rowNumber, "Last Name is required."));
                        continue;
                    }

                    String company = getColumn(record, "Company", "company");
                    String title = getColumn(record, "Title", "title");
                    String jobPosition = getColumn(record, "Job Position", "JobPosition", "job_position");
                    String primaryEmailRaw = getColumn(record, "Primary Email", "PrimaryEmail", "email");
                    String secondaryEmailsRaw = getColumn(record, "Secondary Emails", "SecondaryEmails");
                    String primaryPhoneRaw = getColumn(record, "Primary Phone", "PrimaryPhone", "phone");
                    String secondaryPhonesRaw = getColumn(record, "Secondary Phones", "SecondaryPhones");
                    String address = getColumn(record, "Address", "address");
                    String city = getColumn(record, "City", "city");
                    String state = getColumn(record, "State", "state");
                    String country = getColumn(record, "Country", "country");
                    String postalCode = getColumn(record, "Postal Code", "PostalCode", "postal_code");
                    String birthdayRaw = getColumn(record, "Birthday", "birthday");
                    String notes = getColumn(record, "Notes", "notes");

                    // Duplicate check
                    String nameCompanyKey = (firstName + "|" + lastName + "|" + (company != null ? company : ""))
                            .toLowerCase();
                    boolean isDuplicate = existingNameCompany.contains(nameCompanyKey);

                    String normalizedPrimaryEmail = null;
                    if (!primaryEmailRaw.isBlank()) {
                        try {
                            normalizedPrimaryEmail = EmailNormalizer.normalize(primaryEmailRaw);
                            if (existingEmails.contains(normalizedPrimaryEmail.toLowerCase())) {
                                isDuplicate = true;
                            }
                        } catch (Exception e) {
                            errors.add(new CsvImportSummaryResponse.RowErrorDetail(rowNumber,
                                    "Invalid primary email format: " + primaryEmailRaw));
                            continue;
                        }
                    }

                    if (isDuplicate) {
                        duplicateRows++;
                        continue;
                    }

                    Contact contact = new Contact();
                    contact.setUser(user);
                    contact.setFirstName(firstName);
                    contact.setLastName(lastName);
                    contact.setTitle(title.isBlank() ? null : title);
                    contact.setCompany(company.isBlank() ? null : company);
                    contact.setJobPosition(jobPosition.isBlank() ? null : jobPosition);
                    contact.setAddress(address.isBlank() ? null : address);
                    contact.setCity(city.isBlank() ? null : city);
                    contact.setState(state.isBlank() ? null : state);
                    contact.setCountry(country.isBlank() ? null : country);
                    contact.setPostalCode(postalCode.isBlank() ? null : postalCode);
                    contact.setNotes(notes.isBlank() ? null : notes);

                    if (!birthdayRaw.isBlank()) {
                        try {
                            contact.setBirthday(LocalDate.parse(birthdayRaw.trim()));
                        } catch (Exception e) {
                            // Ignored or lenient parse
                        }
                    }

                    Set<String> seenEmails = new HashSet<>();
                    boolean hasPrimaryEmail = false;
                    if (normalizedPrimaryEmail != null) {
                        contact.addEmail(new ContactEmail(normalizedPrimaryEmail, EmailLabel.WORK, true));
                        seenEmails.add(normalizedPrimaryEmail.toLowerCase(Locale.ROOT));
                        hasPrimaryEmail = true;
                        existingEmails.add(normalizedPrimaryEmail.toLowerCase(Locale.ROOT));
                    }

                    if (!secondaryEmailsRaw.isBlank()) {
                        String[] parts = secondaryEmailsRaw.split("[;,]");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (trimmed.isBlank()) {
                                continue;
                            }
                            String normalized = EmailNormalizer.normalize(trimmed);
                            String dedupeKey = normalized.toLowerCase(Locale.ROOT);
                            if (seenEmails.contains(dedupeKey)) {
                                continue;
                            }
                            seenEmails.add(dedupeKey);
                            contact.addEmail(new ContactEmail(normalized, EmailLabel.OTHER, false));
                        }
                    }

                    if (!hasPrimaryEmail && !contact.getEmails().isEmpty()) {
                        contact.getEmails().get(0).setPrimary(true);
                    }

                    Set<String> seenPhones = new HashSet<>();
                    boolean hasPrimaryPhone = false;
                    if (!primaryPhoneRaw.isBlank()) {
                        String normalizedPhone = PhoneNormalizer.normalize(primaryPhoneRaw);
                        contact.addPhoneNumber(new ContactPhoneNumber(normalizedPhone, PhoneLabel.WORK, true));
                        seenPhones.add(normalizedPhone.toLowerCase(Locale.ROOT));
                        hasPrimaryPhone = true;
                    }

                    if (!secondaryPhonesRaw.isBlank()) {
                        String[] parts = secondaryPhonesRaw.split("[;,]");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (trimmed.isBlank()) {
                                continue;
                            }
                            String normalizedPhone = PhoneNormalizer.normalize(trimmed);
                            String dedupeKey = normalizedPhone.toLowerCase(Locale.ROOT);
                            if (seenPhones.contains(dedupeKey)) {
                                continue;
                            }
                            seenPhones.add(dedupeKey);
                            contact.addPhoneNumber(new ContactPhoneNumber(normalizedPhone, PhoneLabel.OTHER, false));
                        }
                    }

                    if (!hasPrimaryPhone && !contact.getPhoneNumbers().isEmpty()) {
                        contact.getPhoneNumbers().get(0).setPrimary(true);
                    }

                    contactsToSave.add(contact);
                    existingNameCompany.add(nameCompanyKey);
                    importedRows++;

                } catch (Exception rowEx) {
                    errors.add(new CsvImportSummaryResponse.RowErrorDetail(rowNumber,
                            "Error processing row: " + rowEx.getMessage()));
                }
            }

            if (!contactsToSave.isEmpty()) {
                contactRepository.saveAll(contactsToSave);
            }

            log.info("CSV import completed: total={}, imported={}, duplicates={}, errors={}",
                    totalRows, importedRows, duplicateRows, errors.size());

            return new CsvImportSummaryResponse(totalRows, importedRows, duplicateRows, errors.size(), errors);

        } catch (InvalidCsvFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file for userId={}", userId, e);
            throw new FileProcessingException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    private String getColumn(CSVRecord record, String... possibleNames) {
        for (String name : possibleNames) {
            String normalizedName = normalizeHeader(name);
            for (String header : record.toMap().keySet()) {
                if (normalizeHeader(header).equals(normalizedName)) {
                    String val = record.get(header);
                    return val != null ? val.trim() : "";
                }
            }
        }
        return "";
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
