package com.conceptarena.web.rest.dto;

import java.time.Instant;

public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private Instant timestamp;

    private ErrorResponse() {}

    public static ErrorResponse of(int status, String error, String message) {
        ErrorResponse response = new ErrorResponse();
        response.status = status;
        response.error = error;
        response.message = message;
        response.timestamp = Instant.now();
        return response;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
