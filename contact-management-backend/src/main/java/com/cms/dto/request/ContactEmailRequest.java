package com.cms.dto.request;

import com.cms.entity.EmailLabel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactEmailRequest(
        @NotBlank(message = "Email address is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        EmailLabel label,

        boolean isPrimary
) {
}
