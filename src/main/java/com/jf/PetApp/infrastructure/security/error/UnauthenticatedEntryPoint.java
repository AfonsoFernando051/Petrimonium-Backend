package com.jf.PetApp.infrastructure.security.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers 401 for a request that arrived with no usable authentication — no bearer token, or a
 * token that failed signature/expiry validation in {@code JwtAuthenticationFilter}.
 *
 * <p>Registering this explicitly is not cosmetic. With neither {@code httpBasic} nor
 * {@code formLogin} enabled (both are disabled in {@code SecurityConfig}), Spring Security's
 * fallback entry point is {@code Http403ForbiddenEntryPoint}, so an expired access token used to
 * answer <b>403</b>. The mobile client's {@code ApiClient._sendWithAuth} refreshes and retries on
 * exactly 401 and returns anything else untouched, so every session silently died at
 * {@code jwt.expiration} (one hour) instead of living the refresh token's 30 days — the refresh
 * flow existed but could never fire.
 */
public class UnauthenticatedEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProblemDetailWriter writer;

    public UnauthenticatedEntryPoint(ObjectMapper objectMapper) {
        this.writer = new SecurityProblemDetailWriter(objectMapper);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writer.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Authentication is required. Sign in again or refresh your session.");
    }
}
