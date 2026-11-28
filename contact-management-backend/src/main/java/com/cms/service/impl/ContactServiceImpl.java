package com.cms.service.impl;

import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.dto.response.PageResponse;
import com.cms.entity.Contact;
import com.cms.entity.User;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.ContactMapper;
import com.cms.repository.ContactRepository;
import com.cms.repository.ContactSpecifications;
import com.cms.repository.UserRepository;
import com.cms.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactMapper contactMapper;

    public ContactServiceImpl(ContactRepository contactRepository,
            UserRepository userRepository,
            ContactMapper contactMapper) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.contactMapper = contactMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactSummaryResponse> getContacts(Long userId, String query, String company, String city,
            Pageable pageable) {
        log.debug("Fetching contacts: userId={} query={} page={} size={}", userId, query, pageable.getPageNumber(),
                pageable.getPageSize());

        Specification<Contact> spec = ContactSpecifications.forUserAndSearch(userId, query, company, city);
        Page<Contact> page = contactRepository.findAll(spec, pageable);

        Page<ContactSummaryResponse> mapped = page.map(contactMapper::toSummary);
        return PageResponse.from(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactDetailResponse getContactById(Long contactId, Long userId) {
        Contact contact = contactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + contactId));

        return contactMapper.toDetail(contact);
    }

    @Override
    @Transactional
    public ContactDetailResponse createContact(Long userId, CreateContactRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Contact contact = contactMapper.toEntity(request, user);
        Contact saved = contactRepository.save(contact);
        log.info("Contact created: contactId={} userId={}", saved.getId(), userId);

        return contactMapper.toDetail(saved);
    }

    @Override
    @Transactional
    public ContactDetailResponse updateContact(Long contactId, Long userId, CreateContactRequest request) {
        Contact contact = contactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + contactId));

        contactMapper.updateEntity(contact, request);
        Contact updated = contactRepository.save(contact);
        log.info("Contact updated: contactId={} userId={}", updated.getId(), userId);

        return contactMapper.toDetail(updated);
    }

    @Override
    @Transactional
    public void deleteContact(Long contactId, Long userId) {
        if (!contactRepository.existsByIdAndUserId(contactId, userId)) {
            throw new ResourceNotFoundException("Contact not found with id: " + contactId);
        }

        contactRepository.deleteByIdAndUserId(contactId, userId);
        log.info("Contact deleted: contactId={} userId={}", contactId, userId);
    }
}
