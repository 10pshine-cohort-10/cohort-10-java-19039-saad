package com.cms.service;

import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ContactService {

    PageResponse<ContactSummaryResponse> getContacts(Long userId, String query, String company, String city, Pageable pageable);

    ContactDetailResponse getContactById(Long contactId, Long userId);

    ContactDetailResponse createContact(Long userId, CreateContactRequest request);

    ContactDetailResponse updateContact(Long contactId, Long userId, CreateContactRequest request);

    void deleteContact(Long contactId, Long userId);
}
