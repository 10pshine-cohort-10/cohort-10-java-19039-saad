package com.cms.exception;

import com.cms.dto.response.FieldErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Base type for all expected, client-facing failures. Mapped to {@code ApiError} by the global handler. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient List<FieldErrorDetail> fieldErrors;

    protected ApiException(HttpStatus status, String code, String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }
}
