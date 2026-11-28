package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email or phone number is required")
        @Size(max = 254, message = "Identifier is too long")
        String identifier,

        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password is too long")
        String password) {
}
