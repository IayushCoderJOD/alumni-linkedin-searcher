package com.alumni.searcher.exception;

import org.springframework.http.HttpStatus;

public class ProfileSourceException extends RuntimeException {

    private final HttpStatus status;

    public ProfileSourceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ProfileSourceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
