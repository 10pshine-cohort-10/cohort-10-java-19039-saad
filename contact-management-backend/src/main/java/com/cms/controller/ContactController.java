package com.cms.controller;

import com.cms.dto.request.CreateContactRequest;
import com.cms.dto.response.ContactDetailResponse;
import com.cms.dto.response.ContactSummaryResponse;
import com.cms.dto.response.PageResponse;
import com.cms.security.UserPrincipal;
import com.cms.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "Get paginated contacts with search and filtering for authenticated user")
    public PageResponse<ContactSummaryResponse> getContacts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String city,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return contactService.getContacts(principal.id(), query, company, city, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed contact profile by ID")
    public ContactDetailResponse getContactById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return contactService.getContactById(id, principal.id());
    }

    @PostMapping
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ContactDetailResponse> createContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateContactRequest request) {
        ContactDetailResponse response = contactService.createContact(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing contact")
    public ContactDetailResponse updateContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateContactRequest request) {
        return contactService.updateContact(id, principal.id(), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact by ID")
    public ResponseEntity<Void> deleteContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        contactService.deleteContact(id, principal.id());
        return ResponseEntity.noContent().build();
    }
}
