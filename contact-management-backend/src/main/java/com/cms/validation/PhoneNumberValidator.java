package com.cms.validation;

import com.cms.util.PhoneNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // reported by @NotBlank
        }
        return PhoneNormalizer.isValid(value);
    }
}
