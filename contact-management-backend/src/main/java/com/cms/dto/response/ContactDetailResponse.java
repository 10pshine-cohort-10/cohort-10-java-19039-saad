package com.cms.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContactDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String title,
        String company,
        String jobPosition,
        String notes,
        String avatarUrl,
        String website,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        LocalDate birthday,
        List<ContactEmailResponse> emails,
        List<ContactPhoneResponse> phoneNumbers,
        Instant createdAt,
        Instant updatedAt
) {
}
