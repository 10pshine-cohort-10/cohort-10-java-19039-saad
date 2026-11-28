package com.cms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateContactRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 150, message = "Company cannot exceed 150 characters")
        String company,

        @Size(max = 100, message = "Job position cannot exceed 100 characters")
        String jobPosition,

        String notes,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        String avatarUrl,

        @Size(max = 255, message = "Website cannot exceed 255 characters")
        String website,

        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode,

        LocalDate birthday,

        @Valid
        List<ContactEmailRequest> emails,

        @Valid
        List<ContactPhoneRequest> phoneNumbers
) {
}
