package com.cms.dto.response;

import com.cms.entity.PhoneLabel;

public record ContactPhoneResponse(
        Long id,
        String phoneNumber,
        PhoneLabel label,
        boolean isPrimary
) {
}
