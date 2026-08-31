package com.jf.PetApp.core.security;

import com.jf.PetApp.core.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        // Never let one test's authentication leak into the next.
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserEmail_WithADomainUserPrincipal_ReturnsItsEmail() {
        User user = new User();
        user.setEmail("investor@test.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("investor@test.com", SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void getCurrentUserEmail_WithAUserDetailsPrincipal_ReturnsItsUsername() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("investor@test.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("investor@test.com", SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void getCurrentUserEmail_WithAStringPrincipal_ReturnsItDirectly() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("investor@test.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("investor@test.com", SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void getCurrentUserEmail_WithNoAuthenticationInContext_ThrowsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, SecurityUtils::getCurrentUserEmail);
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void getCurrentUserEmail_WithAnUnauthenticatedToken_ThrowsUnauthorized() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("investor@test.com", null);
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(ResponseStatusException.class, SecurityUtils::getCurrentUserEmail);
    }

    @Test
    void getCurrentUserEmail_WithAnUnrecognizedPrincipalType_ThrowsUnauthorized() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(new Object(), null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(ResponseStatusException.class, SecurityUtils::getCurrentUserEmail);
    }
}
