package com.cms.service.impl;

import com.cms.dto.response.DashboardSummaryResponse;
import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.repository.ContactRepository;
import com.cms.service.DashboardService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final ContactRepository contactRepository;

    public DashboardServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        log.debug("Dashboard summary requested: userId={}", userId);

        Instant now = Instant.now();
        Instant startOfMonth = now.minus(30, ChronoUnit.DAYS);
        Instant startOfWeek = now.minus(7, ChronoUnit.DAYS);

        long totalContacts = contactRepository.countByUserId(userId);
        long contactsThisMonth = contactRepository.countByUserIdAndCreatedAtAfter(userId, startOfMonth);
        long contactsThisWeek = contactRepository.countByUserIdAndCreatedAtAfter(userId, startOfWeek);

        List<Contact> recent = contactRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);

        List<DashboardSummaryResponse.RecentContactResponse> recentResponses = recent.stream()
                .map(c -> {
                    String primaryEmail = c.getEmails().stream()
                            .filter(ContactEmail::isPrimary)
                            .map(ContactEmail::getEmail)
                            .findFirst()
                            .orElse(c.getEmails().isEmpty() ? null : c.getEmails().get(0).getEmail());

                    String primaryPhone = c.getPhoneNumbers().stream()
                            .filter(ContactPhoneNumber::isPrimary)
                            .map(ContactPhoneNumber::getPhoneNumber)
                            .findFirst()
                            .orElse(c.getPhoneNumbers().isEmpty() ? null : c.getPhoneNumbers().get(0).getPhoneNumber());

                    return new DashboardSummaryResponse.RecentContactResponse(
                            c.getId(),
                            c.getFirstName(),
                            c.getLastName(),
                            c.getCompany(),
                            primaryEmail,
                            primaryPhone,
                            c.getCreatedAt().toString()
                    );
                })
                .toList();

        return new DashboardSummaryResponse(totalContacts, contactsThisMonth, contactsThisWeek, recentResponses);
    }
}
