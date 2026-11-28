package com.cms.dto.response;

import java.time.Instant;

public record ContactSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String title,
        String company,
        String primaryEmail,
        String primaryPhone,
        Instant createdAt,
        Instant updatedAt
) {
}
