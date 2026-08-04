package com.epiis.projectcasaketteler.exception;

import org.springframework.http.HttpStatus;

public class DocumentAccessException extends RuntimeException {
    private final HttpStatus status;

    public DocumentAccessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}