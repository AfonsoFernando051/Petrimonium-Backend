package com.jf.PetApp.infrastructure.security;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stamps every request with a correlation id (accepting an inbound X-Request-Id from a
 * reverse proxy/load balancer, generating one otherwise), puts it in the logging MDC so
 * every log line for this request can be tied together, and echoes it back in the response.
 * Without this, correlating log lines for a single failed request across a real (or
 * multi-instance) deployment is guesswork.
 *
 * <p>The inbound header is only reused when it already looks like a correlation id
 * ({@link #SAFE_REQUEST_ID}); anything else is replaced with a generated UUID rather than
 * sanitized in place, so there is no half-trusted value anywhere. That check is not decorative:
 * {@code app.security.trusted-proxies} is empty by default, meaning no hop in front of this app
 * is trusted, so this header is attacker-controlled on every request — and it flows straight
 * into the logging MDC, where an embedded newline forges entire log lines and can bury or
 * fabricate an audit trail.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "requestId";
    public static final String HEADER_NAME = "X-Request-Id";

    // Wide enough for every correlation id format a real proxy emits (UUIDs, W3C traceparent,
    // Heroku/CloudFront/ALB request ids) and narrow enough to exclude CR/LF and the control
    // characters that make log forging possible. The 200-char cap keeps an inbound header from
    // bloating every log line for its request.
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,200}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(String inbound) {
        if (inbound != null && SAFE_REQUEST_ID.matcher(inbound).matches()) {
            return inbound;
        }
        return UUID.randomUUID().toString();
    }
}
