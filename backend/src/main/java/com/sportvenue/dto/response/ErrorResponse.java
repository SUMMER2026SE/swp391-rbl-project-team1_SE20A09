package com.sportvenue.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard error response cho API.
 */
public class ErrorResponse {
    private final int status;
    private final String message;
    private final List<String> errors;
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String message, List<String> errors, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors != null ? errors : Collections.emptyList();
        this.timestamp = timestamp;
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Collections.emptyList(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, List<String> errors) {
        return new ErrorResponse(status, message, errors, LocalDateTime.now());
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
