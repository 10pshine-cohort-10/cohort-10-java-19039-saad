package com.cms.dto.response;

import java.util.List;

public record DashboardSummaryResponse(
        long totalContacts,
        long contactsThisMonth,
        long contactsThisWeek,
        List<RecentContactResponse> recentContacts
) {

    public record RecentContactResponse(
            Long id,
            String firstName,
            String lastName,
            String company,
            String primaryEmail,
            String primaryPhone,
            String createdAt
    ) {}
}
