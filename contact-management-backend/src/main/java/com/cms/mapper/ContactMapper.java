package com.cms.mapper;

import com.cms.dto.request.ContactEmailRequest;
import com.cms.dto.request.ContactPhoneRequest;
import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactEmailResponse;
import com.cms.dto.response.ContactPhoneResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.entity.EmailLabel;
import com.cms.entity.PhoneLabel;
import com.cms.entity.User;
import com.cms.util.EmailNormalizer;
import com.cms.util.PhoneNormalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public Contact toEntity(CreateContactRequest request, User user) {
        Contact contact = new Contact();
        contact.setUser(user);
        applyCommonFields(contact, request);

        applyEmailValues(contact, request.emails());
        applyPhoneValues(contact, request.phoneNumbers());

        return contact;
    }

    public void updateEntity(Contact contact, CreateContactRequest request) {
        applyCommonFields(contact, request);

        reconcileEmailValues(contact, request.emails());
        reconcilePhoneValues(contact, request.phoneNumbers());
    }

    private void applyEmailValues(Contact contact, List<ContactEmailRequest> emails) {
        if (emails == null) {
            return;
        }

        Set<String> seen = new HashSet<>();
        boolean hasPrimary = false;

        for (ContactEmailRequest emailReq : emails) {
            if (emailReq == null || emailReq.email() == null || emailReq.email().isBlank()) {
                continue;
            }

            String normalizedEmail = EmailNormalizer.normalize(emailReq.email());
            String key = normalizedEmail.toLowerCase(Locale.ROOT);
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);

            boolean isPrimary = emailReq.isPrimary() && !hasPrimary;
            if (isPrimary) {
                hasPrimary = true;
            }

            contact.addEmail(new ContactEmail(
                    normalizedEmail,
                    emailReq.label() != null ? emailReq.label() : EmailLabel.WORK,
                    isPrimary));
        }

        if (!hasPrimary && !contact.getEmails().isEmpty()) {
            contact.getEmails().get(0).setPrimary(true);
        }
    }

    private void applyPhoneValues(Contact contact, List<ContactPhoneRequest> phoneNumbers) {
        if (phoneNumbers == null) {
            return;
        }

        Set<String> seen = new HashSet<>();
        boolean hasPrimary = false;

        for (ContactPhoneRequest phoneReq : phoneNumbers) {
            if (phoneReq == null || phoneReq.phoneNumber() == null || phoneReq.phoneNumber().isBlank()) {
                continue;
            }

            String normalizedPhone = PhoneNormalizer.normalize(phoneReq.phoneNumber());
            String key = normalizedPhone.toLowerCase(Locale.ROOT);
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);

            boolean isPrimary = phoneReq.isPrimary() && !hasPrimary;
            if (isPrimary) {
                hasPrimary = true;
            }

            contact.addPhoneNumber(new ContactPhoneNumber(
                    normalizedPhone,
                    phoneReq.label() != null ? phoneReq.label() : PhoneLabel.WORK,
                    isPrimary));
        }

        if (!hasPrimary && !contact.getPhoneNumbers().isEmpty()) {
            contact.getPhoneNumbers().get(0).setPrimary(true);
        }
    }

    public ContactSummaryResponse toSummary(Contact contact) {
        String primaryEmail = contact.getEmails().stream()
                .filter(ContactEmail::isPrimary)
                .map(ContactEmail::getEmail)
                .findFirst()
                .orElse(contact.getEmails().isEmpty() ? null : contact.getEmails().get(0).getEmail());

        String primaryPhone = contact.getPhoneNumbers().stream()
                .filter(ContactPhoneNumber::isPrimary)
                .map(ContactPhoneNumber::getPhoneNumber)
                .findFirst()
                .orElse(contact.getPhoneNumbers().isEmpty() ? null : contact.getPhoneNumbers().get(0).getPhoneNumber());

        return new ContactSummaryResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getTitle(),
                contact.getCompany(),
                primaryEmail,
                primaryPhone,
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }

    public ContactDetailResponse toDetail(Contact contact) {
        List<ContactEmailResponse> emailResponses = contact.getEmails() != null
                ? contact.getEmails().stream()
                        .map(e -> new ContactEmailResponse(e.getId(), e.getEmail(), e.getLabel(), e.isPrimary()))
                        .toList()
                : Collections.emptyList();

        List<ContactPhoneResponse> phoneResponses = contact.getPhoneNumbers() != null
                ? contact.getPhoneNumbers().stream()
                        .map(p -> new ContactPhoneResponse(p.getId(), p.getPhoneNumber(), p.getLabel(), p.isPrimary()))
                        .toList()
                : Collections.emptyList();

        return new ContactDetailResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getTitle(),
                contact.getCompany(),
                contact.getJobPosition(),
                contact.getNotes(),
                contact.getAvatarUrl(),
                contact.getWebsite(),
                contact.getAddress(),
                contact.getCity(),
                contact.getState(),
                contact.getCountry(),
                contact.getPostalCode(),
                contact.getBirthday(),
                emailResponses,
                phoneResponses,
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }

    private void applyCommonFields(Contact contact, CreateContactRequest request) {
        contact.setFirstName(request.firstName() != null ? request.firstName().trim() : "");
        contact.setLastName(request.lastName() != null ? request.lastName().trim() : "");
        contact.setTitle(trimOrNull(request.title()));
        contact.setCompany(trimOrNull(request.company()));
        contact.setJobPosition(trimOrNull(request.jobPosition()));
        contact.setNotes(trimOrNull(request.notes()));
        contact.setAvatarUrl(trimOrNull(request.avatarUrl()));
        contact.setWebsite(trimOrNull(request.website()));
        contact.setAddress(trimOrNull(request.address()));
        contact.setCity(trimOrNull(request.city()));
        contact.setState(trimOrNull(request.state()));
        contact.setCountry(trimOrNull(request.country()));
        contact.setPostalCode(trimOrNull(request.postalCode()));
        contact.setBirthday(request.birthday());
    }

    private String trimOrNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void reconcileEmailValues(Contact contact, List<ContactEmailRequest> emails) {
        List<ContactEmail> existing = new ArrayList<>(contact.getEmails());
        Set<String> seen = new HashSet<>();
        List<ContactEmail> desired = new ArrayList<>();

        if (emails != null) {
            for (ContactEmailRequest emailReq : emails) {
                if (emailReq == null || emailReq.email() == null || emailReq.email().isBlank()) {
                    continue;
                }

                String normalizedEmail = EmailNormalizer.normalize(emailReq.email());
                String key = normalizedEmail.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    continue;
                }

                ContactEmail email = existing.stream()
                        .filter(item -> EmailNormalizer.normalize(item.getEmail()).toLowerCase(Locale.ROOT).equals(key))
                        .findFirst()
                        .orElse(new ContactEmail());

                if (email.getContact() == null) {
                    email.setEmail(normalizedEmail);
                    email.setLabel(emailReq.label() != null ? emailReq.label() : EmailLabel.WORK);
                    email.setPrimary(false);
                    contact.addEmail(email);
                } else {
                    email.setEmail(normalizedEmail);
                    email.setLabel(emailReq.label() != null ? emailReq.label() : EmailLabel.WORK);
                    email.setPrimary(false);
                }
                desired.add(email);
            }
        }

        for (ContactEmail email : existing) {
            if (!desired.contains(email)) {
                contact.removeEmail(email);
            }
        }

        if (!desired.isEmpty()) {
            boolean primarySet = false;
            for (int i = 0; i < desired.size(); i++) {
                ContactEmail email = desired.get(i);
                if (i == 0) {
                    email.setPrimary(true);
                } else {
                    email.setPrimary(false);
                }
            }

            if (emails != null) {
                for (int i = 0; i < emails.size(); i++) {
                    ContactEmailRequest emailReq = emails.get(i);
                    if (emailReq == null || emailReq.email() == null || emailReq.email().isBlank()) {
                        continue;
                    }
                    if (!emailReq.isPrimary()) {
                        continue;
                    }
                    String key = EmailNormalizer.normalize(emailReq.email()).toLowerCase(Locale.ROOT);
                    for (ContactEmail email : desired) {
                        if (EmailNormalizer.normalize(email.getEmail()).toLowerCase(Locale.ROOT).equals(key)) {
                            for (ContactEmail item : desired) {
                                item.setPrimary(false);
                            }
                            email.setPrimary(true);
                            primarySet = true;
                            break;
                        }
                    }
                    if (primarySet) {
                        break;
                    }
                }
            }
        }
    }

    private void reconcilePhoneValues(Contact contact, List<ContactPhoneRequest> phoneNumbers) {
        List<ContactPhoneNumber> existing = new ArrayList<>(contact.getPhoneNumbers());
        Set<String> seen = new HashSet<>();
        List<ContactPhoneNumber> desired = new ArrayList<>();

        if (phoneNumbers != null) {
            for (ContactPhoneRequest phoneReq : phoneNumbers) {
                if (phoneReq == null || phoneReq.phoneNumber() == null || phoneReq.phoneNumber().isBlank()) {
                    continue;
                }

                String normalizedPhone = PhoneNormalizer.normalize(phoneReq.phoneNumber());
                String key = normalizedPhone.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    continue;
                }

                ContactPhoneNumber phone = existing.stream()
                        .filter(item -> PhoneNormalizer.normalize(item.getPhoneNumber()).toLowerCase(Locale.ROOT)
                                .equals(key))
                        .findFirst()
                        .orElse(new ContactPhoneNumber());

                if (phone.getContact() == null) {
                    phone.setPhoneNumber(normalizedPhone);
                    phone.setLabel(phoneReq.label() != null ? phoneReq.label() : PhoneLabel.WORK);
                    phone.setPrimary(false);
                    contact.addPhoneNumber(phone);
                } else {
                    phone.setPhoneNumber(normalizedPhone);
                    phone.setLabel(phoneReq.label() != null ? phoneReq.label() : PhoneLabel.WORK);
                    phone.setPrimary(false);
                }
                desired.add(phone);
            }
        }

        for (ContactPhoneNumber phone : existing) {
            if (!desired.contains(phone)) {
                contact.removePhoneNumber(phone);
            }
        }

        if (!desired.isEmpty()) {
            for (int i = 0; i < desired.size(); i++) {
                desired.get(i).setPrimary(i == 0);
            }

            boolean primarySet = false;
            if (phoneNumbers != null) {
                for (ContactPhoneRequest phoneReq : phoneNumbers) {
                    if (phoneReq == null || phoneReq.phoneNumber() == null || phoneReq.phoneNumber().isBlank()
                            || !phoneReq.isPrimary()) {
                        continue;
                    }
                    String key = PhoneNormalizer.normalize(phoneReq.phoneNumber()).toLowerCase(Locale.ROOT);
                    for (ContactPhoneNumber phone : desired) {
                        if (PhoneNormalizer.normalize(phone.getPhoneNumber()).toLowerCase(Locale.ROOT).equals(key)) {
                            for (ContactPhoneNumber item : desired) {
                                item.setPrimary(false);
                            }
                            phone.setPrimary(true);
                            primarySet = true;
                            break;
                        }
                    }
                    if (primarySet) {
                        break;
                    }
                }
            }
        }
    }
}
