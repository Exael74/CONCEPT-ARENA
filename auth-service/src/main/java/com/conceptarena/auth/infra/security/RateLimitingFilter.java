package com.conceptarena.auth.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate-limits the auth endpoints (login/register) per client IP. The counting itself lives in
 * {@link AuthRateLimiter} — in-memory by default, Redis-backed (shared across replicas) under the
 * docker profile — so this filter only maps requests to a client key and turns a rejection into 429.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Include the OTP endpoints so codes can't be requested/brute-forced at high rate per IP (on top
    // of the per-email attempt cap in OtpStore).
    private static final Set<String> LIMITED_PATHS = Set.of(
        "/api/auth/login", "/api/auth/register", "/api/auth/otp/request", "/api/auth/otp/verify");

    private final AuthRateLimiter rateLimiter;

    public RateLimitingFilter(AuthRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!LIMITED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!rateLimiter.allow(clientKey(request))) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests, try again later\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return StringUtils.hasText(forwardedFor) ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }
}
