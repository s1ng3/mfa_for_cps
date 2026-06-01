package com.cps.mfa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * Stateless (custom-token) security. We disable Spring's own session management and CSRF because
 * authentication is carried by the opaque Bearer token validated in {@link SessionAuthenticationFilter}.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: the very first step of login, health and the H2 console.
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/health", "/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Everything else requires at least a valid (pending or active) session.
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    objectMapper.writeValue(res.getWriter(),
                            Map.of("status", 401, "code", "UNAUTHORIZED",
                                    "message", "Authentication required"));
                }))
                // Allow the H2 console to render in a frame (dev only).
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Prevent Spring Boot from auto-registering the session filter as a top-level servlet filter —
     * it should only run inside the Spring Security chain (added above), not twice.
     */
    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> disableSessionFilterAutoRegistration(
            SessionAuthenticationFilter filter) {
        FilterRegistrationBean<SessionAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
