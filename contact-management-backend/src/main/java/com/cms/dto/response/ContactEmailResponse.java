package com.cms.dto.response;

import com.cms.entity.EmailLabel;

public record ContactEmailResponse(
        Long id,
        String email,
        EmailLabel label,
        boolean isPrimary
) {
}
