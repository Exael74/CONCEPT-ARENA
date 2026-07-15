package com.conceptarena.web.rest;

import com.conceptarena.web.rest.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Safety net for exceptions that escape a controller's own try/catch (or controllers with
 * none at all, e.g. the plain GET endpoints). Previously an uncaught exception here produced
 * Spring Boot's default whitelabel HTML error page with no log line and no trace of it ever
 * happening — this at least logs it (with the request's correlationId, via MDC) and returns
 * the same ApiResponse envelope every other endpoint uses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception reached GlobalExceptionHandler", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error"));
    }
}
