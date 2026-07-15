package com.conceptarena.game.infra.security;

import com.conceptarena.jwtlib.JwtBearerAuthenticationFilter;
import com.conceptarena.jwtlib.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // Browsers cannot set the Authorization header on a WS upgrade request — WS auth
                // is enforced per-endpoint by WsJwtHandshakeInterceptor instead.
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(correlationIdFilter, JwtBearerAuthenticationFilter.class);
        return http.build();
    }
}
