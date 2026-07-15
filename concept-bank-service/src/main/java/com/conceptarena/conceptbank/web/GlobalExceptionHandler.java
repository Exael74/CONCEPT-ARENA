package com.conceptarena.conceptbank.web;

import com.conceptarena.conceptbank.web.dto.ApiResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Safety net for exceptions that escape a controller's own try/catch. Also increments
 * errors_total{service,type} so error rate is visible as a metric, not just a log line
 * (audit gap: "Error Rate" was previously unmeasured — GlobalExceptionHandler only logged).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SERVICE_NAME = "concept-bank-service";

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception reached GlobalExceptionHandler", e);
        Counter.builder("errors_total")
            .tag("service", SERVICE_NAME)
            .tag("type", e.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error"));
    }
}
