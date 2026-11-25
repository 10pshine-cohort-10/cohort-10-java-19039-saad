package com.cms.exception;

import com.cms.dto.response.FieldErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message, List<FieldErrorDetail> fieldErrors) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message, fieldErrors);
    }
}
