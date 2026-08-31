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
