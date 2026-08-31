package com.jf.PetApp.infrastructure.security.jwt;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User userWith(String email, RoleEnum role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @Test
    void doFilterInternal_WithValidBearerToken_SetsAuthenticationOnSecurityContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validate("valid-token")).thenReturn(true);
        when(tokenProvider.extractSubject("valid-token")).thenReturn("investor@test.com");
        when(userRepository.findByEmail("investor@test.com"))
                .thenReturn(Optional.of(userWith("investor@test.com", RoleEnum.USER)));

        filter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertEquals("investor@test.com", ((User) auth.getPrincipal()).getEmail());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithAdminRole_GrantsRoleAdminAuthority() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(tokenProvider.validate("admin-token")).thenReturn(true);
        when(tokenProvider.extractSubject("admin-token")).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(userWith("admin@test.com", RoleEnum.ADMIN)));

        filter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_WithoutAuthorizationHeader_LeavesContextEmptyButContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void doFilterInternal_WithNonBearerAuthorizationHeader_LeavesContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void doFilterInternal_WithInvalidToken_LeavesContextEmptyButContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(tokenProvider.validate("bad-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(tokenProvider, never()).extractSubject(any());
    }

    @Test
    void doFilterInternal_WithValidTokenButUnknownUser_LeavesContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validate("valid-token")).thenReturn(true);
        when(tokenProvider.extractSubject("valid-token")).thenReturn("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
