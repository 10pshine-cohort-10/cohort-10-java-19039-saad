package com.cms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A phone number with 7-15 digits and an optional leading '+'. Spaces, dashes and brackets are tolerated. */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "Enter a valid phone number (7-15 digits, optional leading +)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
