package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.infrastructure.security.RequestIdFilter;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.jf.PetApp.infrastructure.security.ratelimit.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

// Enables @PreAuthorize/@PostAuthorize/hasRole(...) on individual use cases or controller
// methods. No endpoint uses it yet — the ADMIN role in RoleEnum has no admin-only
// functionality behind it today (verified: no route in this codebase should currently be
// admin-restricted). This annotation is forward-compatible groundwork only, so that the day
// an admin-only endpoint is actually added, protecting it is a one-line @PreAuthorize instead
// of first having to remember to wire up method security at all.
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    // Comma-separated list of allowed origins, environment-specific. Blank by default — the
    // mobile app doesn't need CORS at all (native HTTP client, not a browser), so an
    // unconfigured environment gets no cross-origin access rather than silently defaulting to
    // permissive. application-dev.properties sets the wildcard for local browser tooling (H2
    // console etc.); application-prod.properties requires a real origin with no fallback.
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    // Same flag that gates the H2 console itself (application.properties vs
    // application-prod.properties) — the only reason X-Frame-Options was ever disabled was to
    // let the H2 console render in an iframe, so it's disabled only where that console is
    // actually enabled. Everywhere else (including prod) keeps the safe same-origin default.
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        TokenProvider tokenProvider,
        UserRepository userRepository
    ) {
        return new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(
        @Value("${app.security.trusted-proxies:}") String trustedProxies
    ) {
        return new RateLimitingFilter(trustedProxies);
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RateLimitingFilter rateLimitingFilter,
        RequestIdFilter requestIdFilter
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .headers(headers -> headers
                .frameOptions(frame -> {
                    if (h2ConsoleEnabled) {
                        frame.disable();
                    } else {
                        frame.sameOrigin();
                    }
                })
                // A pure JSON API never renders untrusted HTML, so the strictest possible
                // policy is safe — unlike HSTS/X-Frame-Options, Spring Security has no CSP
                // default, so it has to be set explicitly or the header is simply absent.
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            // Explicitly ordered ahead of the JWT filter (not just both "before
            // UsernamePasswordAuthenticationFilter", which wouldn't fix their relative order) —
            // a rate-limited request should never reach JWT parsing at all.
            .addFilterBefore(
                rateLimitingFilter,
                JwtAuthenticationFilter.class
            )
            // Runs first of all three: every request, including rate-limited/rejected ones,
            // should carry a correlation id in its logs and response.
            .addFilterBefore(
                requestIdFilter,
                RateLimitingFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        // An unconfigured/blank value now means "no cross-origin access" (empty list), not a
        // silent wildcard — see the allowedOrigins field doc above for why.
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
