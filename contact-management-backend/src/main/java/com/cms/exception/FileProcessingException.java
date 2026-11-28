package com.cms.exception;

import org.springframework.http.HttpStatus;

public class FileProcessingException extends ApiException {

    public FileProcessingException(String message) {
        super(HttpStatus.BAD_REQUEST, "FILE_PROCESSING_ERROR", message, null);
    }
}
