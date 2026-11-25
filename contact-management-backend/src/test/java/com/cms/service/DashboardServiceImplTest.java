package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cms.dto.response.DashboardSummaryResponse;
import com.cms.repository.ContactRepository;
import com.cms.service.impl.DashboardServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(contactRepository);
        when(contactRepository.countByUserId(42L)).thenReturn(0L);
        when(contactRepository.countByUserIdAndCreatedAtAfter(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(contactRepository.findTop5ByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of());
    }

    @Test
    void getSummary_returnsInitialZeroCountersAndEmptyRecentContacts() {
        Long userId = 42L;

        DashboardSummaryResponse response = service.getSummary(userId);

        assertThat(response).isNotNull();
        assertThat(response.totalContacts()).isZero();
        assertThat(response.contactsThisMonth()).isZero();
        assertThat(response.contactsThisWeek()).isZero();
        assertThat(response.recentContacts()).isEmpty();
    }
}
