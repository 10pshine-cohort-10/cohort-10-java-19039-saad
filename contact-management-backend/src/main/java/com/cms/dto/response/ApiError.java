package com.cms.dto.response;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Consistent error body returned for every failed request. Never contains stack traces or SQL details. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public static ApiError of(HttpStatus status, String code, String message, String path,
                              List<FieldErrorDetail> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path,
                fieldErrors == null ? List.of() : fieldErrors);
    }
}
