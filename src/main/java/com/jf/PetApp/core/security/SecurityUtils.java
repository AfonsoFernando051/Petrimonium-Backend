package com.jf.PetApp.core.security;

import com.jf.PetApp.core.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor for utility class
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User domainUser) {
            return domainUser.getEmail();
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            return principalString;
        }

        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User identity not available");
    }
}
