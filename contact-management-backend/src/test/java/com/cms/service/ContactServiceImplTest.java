package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.dto.request.ContactEmailRequest;
import com.cms.dto.request.ContactPhoneRequest;
import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.dto.response.PageResponse;
import com.cms.entity.Contact;
import com.cms.entity.ContactEmail;
import com.cms.entity.ContactPhoneNumber;
import com.cms.entity.EmailLabel;
import com.cms.entity.PhoneLabel;
import com.cms.entity.User;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.ContactMapper;
import com.cms.repository.ContactRepository;
import com.cms.repository.UserRepository;
import com.cms.service.impl.ContactServiceImpl;
import com.cms.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    private ContactMapper contactMapper;
    private ContactServiceImpl service;

    @BeforeEach
    void setUp() {
        contactMapper = new ContactMapper();
        service = new ContactServiceImpl(contactRepository, userRepository, contactMapper);
    }

    @Test
    void createContact_savesAndReturnsDetailResponse() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CreateContactRequest request = new CreateContactRequest(
                "Alice", "Smith", "CTO", "Acme Corp", "Tech Lead", "Key client",
                null, "https://acme.org", "123 Main St", "Springfield", "IL", "USA", "62701",
                null,
                List.of(new ContactEmailRequest("alice@acme.org", EmailLabel.WORK, true)),
                List.of(new ContactPhoneRequest("+15559876543", PhoneLabel.WORK, true)));

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            return c;
        });

        ContactDetailResponse result = service.createContact(userId, request);

        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.company()).isEqualTo("Acme Corp");
        assertThat(result.emails()).hasSize(1);
        assertThat(result.emails().get(0).email()).isEqualTo("alice@acme.org");
        assertThat(result.phoneNumbers()).hasSize(1);
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void updateEntity_keepsOnlySinglePrimaryEmailAndPhone() {
        Contact contact = new Contact();
        contact.setFirstName("Bob");
        contact.setLastName("Jones");

        contact.addEmail(new ContactEmail("old@example.com", EmailLabel.WORK, true));
        contact.addPhoneNumber(new ContactPhoneNumber("+15550000000", PhoneLabel.WORK, true));

        CreateContactRequest request = new CreateContactRequest(
                "Bob", "Jones", "CTO", "Acme", "Engineering", "Notes",
                null, null, null, null, null, null, null, null,
                List.of(
                        new ContactEmailRequest("new@example.com", EmailLabel.WORK, true),
                        new ContactEmailRequest("alternate@example.com", EmailLabel.PERSONAL, true)),
                List.of(
                        new ContactPhoneRequest("+15550000001", PhoneLabel.WORK, true),
                        new ContactPhoneRequest("+15550000002", PhoneLabel.MOBILE, true)));

        contactMapper.updateEntity(contact, request);

        assertThat(contact.getEmails().stream().filter(ContactEmail::isPrimary).count()).isEqualTo(1);
        assertThat(contact.getPhoneNumbers().stream().filter(ContactPhoneNumber::isPrimary).count()).isEqualTo(1);
    }

    @Test
    void getContactById_whenOwnedByUser_returnsDetail() {
        Long userId = 1L;
        Long contactId = 10L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        Contact contact = new Contact();
        contact.setUser(user);
        contact.setFirstName("Bob");
        contact.setLastName("Jones");

        when(contactRepository.findByIdAndUserId(contactId, userId))
                .thenReturn(Optional.of(contact));

        ContactDetailResponse result = service.getContactById(contactId, userId);

        assertThat(result.firstName()).isEqualTo("Bob");
        assertThat(result.lastName()).isEqualTo("Jones");
    }

    @Test
    void getContactById_whenNotOwned_throwsResourceNotFound() {
        Long userId = 1L;
        Long otherContactId = 999L;

        when(contactRepository.findByIdAndUserId(otherContactId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContactById(otherContactId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteContact_whenExistsAndOwned_deletesSuccessfully() {
        Long userId = 1L;
        Long contactId = 10L;

        when(contactRepository.existsByIdAndUserId(contactId, userId)).thenReturn(true);

        service.deleteContact(contactId, userId);

        verify(contactRepository).deleteByIdAndUserId(contactId, userId);
    }

    @Test
    void deleteContact_whenNotOwned_throwsResourceNotFound() {
        Long userId = 1L;
        Long contactId = 999L;

        when(contactRepository.existsByIdAndUserId(contactId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteContact(contactId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getContacts_returnsPaginatedSummaries() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Contact contact = new Contact();
        contact.setFirstName("Carol");
        contact.setLastName("Danvers");
        Page<Contact> page = new PageImpl<>(List.of(contact), pageable, 1);

        when(contactRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<ContactSummaryResponse> result = service.getContacts(userId, "Carol", null, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).firstName()).isEqualTo("Carol");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
