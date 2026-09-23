package com.jf.PetApp.infrastructure.security.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers 403 for a request that authenticated fine but lacks the authority the route requires —
 * today always an app-context mismatch (a Wallet session reaching an Academy route, a session
 * with no {@code app_context} claim at all).
 *
 * <p>The detail stays deliberately generic: naming which authority was missing would tell a
 * caller which contexts exist and which one its token belongs to, and the client has nothing to
 * do with that information — a 403 here is a build-time routing mistake, never something the
 * user can act on.
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemDetailWriter writer;

    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.writer = new SecurityProblemDetailWriter(objectMapper);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writer.write(
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "This session is not allowed to access this resource.");
    }
}
