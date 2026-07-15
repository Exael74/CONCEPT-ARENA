package com.conceptarena.room.infra.security;

import com.conceptarena.jwtlib.JwtBearerAuthenticationFilter;
import com.conceptarena.jwtlib.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorrelationIdFilter correlationIdFilter;

    public SecurityConfig(CorrelationIdFilter correlationIdFilter) {
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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // Browsers cannot set the Authorization header on a WS upgrade request, so this
                // filter chain cannot authenticate the handshake itself — WsJwtHandshakeInterceptor
                // (RawWebSocketConfig) validates a ?token= query param before the socket opens.
                .requestMatchers("/ws/**").permitAll()
                // Listing/reading rooms is public for the lobby; creating/joining/leaving requires auth.
                .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(correlationIdFilter, JwtBearerAuthenticationFilter.class);
        return http.build();
    }
}
