package com.cms.exception;

import com.cms.dto.response.FieldErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Business-rule validation failure (as opposed to bean validation, which is handled separately). */
public class ValidationException extends ApiException {

    public ValidationException(String message, List<FieldErrorDetail> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, fieldErrors);
    }
}
