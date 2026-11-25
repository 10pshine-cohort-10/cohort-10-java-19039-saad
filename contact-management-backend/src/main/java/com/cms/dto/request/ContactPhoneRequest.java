package com.cms.dto.request;

import com.cms.entity.PhoneLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactPhoneRequest(
        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phoneNumber,

        PhoneLabel label,

        boolean isPrimary
) {
}
