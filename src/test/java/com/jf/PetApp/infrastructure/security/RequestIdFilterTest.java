package com.jf.PetApp.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void doFilterInternal_WithInboundRequestIdHeader_ReusesItInsteadOfGeneratingANewOne() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("inbound-request-id");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(RequestIdFilter.HEADER_NAME, "inbound-request-id");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNoInboundHeader_GeneratesAUuid() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(RequestIdFilter.HEADER_NAME),
                org.mockito.ArgumentMatchers.argThat(id -> id != null && id.length() == 36));
    }

    @Test
    void doFilterInternal_WithBlankInboundHeader_GeneratesAUuidInstead() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("   ");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(RequestIdFilter.HEADER_NAME),
                org.mockito.ArgumentMatchers.argThat(id -> id != null && !id.isBlank() && !id.equals("   ")));
    }

    // The inbound header is attacker-controlled on every request: app.security.trusted-proxies
    // is empty by default (see application.properties), so this app trusts no hop in front of it
    // and any client can send whatever it likes here. That value lands in two places that must
    // not take it raw — the logging MDC (a newline forges whole log lines, which is how an
    // attacker hides or fabricates audit trail) and a response header.
    @Test
    void doFilterInternal_WithNewlinesInTheInboundHeader_GeneratesAUuidInstead() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME))
                .thenReturn("abc\n2026-01-01 00:00:00 ERROR forged log line");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(RequestIdFilter.HEADER_NAME),
                org.mockito.ArgumentMatchers.argThat(id -> id.length() == 36 && !id.contains("forged")));
    }

    @Test
    void doFilterInternal_WithAnOverlongInboundHeader_GeneratesAUuidInstead() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("a".repeat(500));

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(RequestIdFilter.HEADER_NAME),
                org.mockito.ArgumentMatchers.argThat(id -> id.length() == 36));
    }

    // A real correlation id from a proxy (UUIDs, W3C traceparent, Heroku/CloudFront request ids)
    // is alphanumerics with dashes/underscores/dots — that shape must keep passing through, or
    // the sanitization would break the only thing this filter is for.
    @Test
    void doFilterInternal_WithATypicalProxyRequestId_StillPassesItThrough() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME))
                .thenReturn("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(RequestIdFilter.HEADER_NAME,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    }

    @Test
    void doFilterInternal_PutsTheRequestIdInMdcWhileTheChainRuns() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("mdc-check-id");

        doAnswer(invocation -> {
            assertEquals("mdc-check-id", MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    void doFilterInternal_RemovesTheMdcEntryAfterTheChainCompletes() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("cleanup-id");

        filter.doFilterInternal(request, response, chain);

        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void doFilterInternal_RemovesTheMdcEntryEvenWhenTheChainThrows() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("error-id");
        doAnswer(invocation -> {
            throw new java.io.IOException("boom");
        }).when(chain).doFilter(any(), any());

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (java.io.IOException expected) {
            // expected propagation
        }

        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }
}
