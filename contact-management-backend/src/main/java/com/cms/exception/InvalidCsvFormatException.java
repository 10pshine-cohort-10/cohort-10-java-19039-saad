package com.cms.exception;

import org.springframework.http.HttpStatus;

public class InvalidCsvFormatException extends ApiException {

    public InvalidCsvFormatException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CSV_FORMAT", message, null);
    }
}
