package com.cms.dto.request;

import com.cms.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmNewPassword
) {
}
