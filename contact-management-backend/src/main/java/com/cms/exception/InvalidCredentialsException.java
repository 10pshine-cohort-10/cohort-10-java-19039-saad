package com.cms.exception;

import com.cms.dto.response.FieldErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        this("Invalid email/phone or password.");
    }

    public InvalidCredentialsException(String message) {
        this(message, null);
    }

    public InvalidCredentialsException(String message, List<FieldErrorDetail> fieldErrors) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message, fieldErrors);
    }
}
