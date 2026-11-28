package com.cms.validation;

/** Implemented by request DTOs that carry a password and its confirmation. */
public interface PasswordConfirmation {

    String password();

    String confirmPassword();
}
