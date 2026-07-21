package com.conceptarena.auth.infra.security;

import com.conceptarena.jwtlib.JwtBearerAuthenticationFilter;
import com.conceptarena.jwtlib.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;
    private final CorrelationIdFilter correlationIdFilter;

    public SecurityConfig(RateLimitingFilter rateLimitingFilter, CorrelationIdFilter correlationIdFilter) {
        this.rateLimitingFilter = rateLimitingFilter;
        this.correlationIdFilter = correlationIdFilter;
    }

    @Bean
    public JwtValidator jwtValidator(
            @Value("${app.jwt.secret:conceptarena-dev-secret-key-must-be-at-least-256-bits-long-for-hs256}") String secret) {
        return new JwtValidator(secret);
    }

    @Bean
    public JwtBearerAuthenticationFilter jwtBearerAuthenticationFilter(JwtValidator jwtValidator) {
        return new JwtBearerAuthenticationFilter(jwtValidator);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtBearerAuthenticationFilter jwtBearerAuthenticationFilter)
            throws Exception {
        http
            // CSRF protection is intentionally disabled: auth is stateless (JWT Bearer token in
            // the Authorization header), no session cookie carries credentials.
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(fo -> fo.disable())) // for H2 console in dev
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Must come before the /api/auth/** permitAll below — first-match-wins ordering.
                .requestMatchers("/api/auth/me/**").authenticated()
                .requestMatchers("/api/auth/**", "/actuator/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, JwtBearerAuthenticationFilter.class)
            .addFilterBefore(correlationIdFilter, RateLimitingFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
