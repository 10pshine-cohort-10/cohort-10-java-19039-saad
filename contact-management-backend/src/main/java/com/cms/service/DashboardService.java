package com.cms.service;

import com.cms.dto.response.DashboardSummaryResponse;

public interface DashboardService {

    /**
     * Returns contact statistics and the most recent contacts for the given user.
     * Until the contacts module is implemented (Checkpoint 3), every counter is zero
     * and the recent-contacts list is empty.
     */
    DashboardSummaryResponse getSummary(Long userId);
}
