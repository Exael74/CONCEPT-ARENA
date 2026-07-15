package com.conceptarena.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot context so @WebMvcTest slices in this module can bootstrap.
 * The real application entry point lives in the bootstrap module; this module has none
 * of its own, and @WebMvcTest needs a @SpringBootConfiguration discoverable from the
 * test class's package upward.
 */
@SpringBootApplication
class TestWebApplication {
}
