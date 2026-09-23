package com.jf.PetApp.infrastructure.security.error;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the same {@link ProblemDetail} shape {@code GlobalExceptionHandler} produces, for the
 * two rejections that never reach it.
 *
 * <p>Authentication and authorization failures are thrown inside the security filter chain,
 * before the {@code DispatcherServlet} runs, so {@code @ControllerAdvice} cannot see them. Left
 * to Spring Security's defaults they answer with an empty body — the only two errors in this API
 * a client would have to parse differently from every other one. Both go through here instead.
 */
public class SecurityProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public SecurityProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        // A rejection that happens this early can still have had a body/status partially
        // committed by something upstream; writing into a committed response would corrupt it.
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", Instant.now().toString());

        response.setStatus(status.value());
        // No explicit charset: Jackson writes UTF-8 to the stream by default, and setting the
        // encoding as well appends ";charset=UTF-8" to the header — a different Content-Type from
        // the one Spring MVC produces for every other ProblemDetail in this API, which is exactly
        // the inconsistency this class exists to avoid.
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
