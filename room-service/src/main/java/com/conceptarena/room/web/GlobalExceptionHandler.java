package com.conceptarena.room.web;

import com.conceptarena.room.web.dto.ApiResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SERVICE_NAME = "room-service";

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Redis write failures (including OOM under maxmemory-policy=noeviction — see ADR-003) are
     * a service-unavailable condition, not an internal bug — 503, not a generic 500.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisFailure(DataAccessException e) {
        log.error("Redis operation failed", e);
        countError(e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error("Room storage temporarily unavailable, try again shortly"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception reached GlobalExceptionHandler", e);
        countError(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error"));
    }

    private void countError(Exception e) {
        Counter.builder("errors_total")
            .tag("service", SERVICE_NAME)
            .tag("type", e.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }
}
