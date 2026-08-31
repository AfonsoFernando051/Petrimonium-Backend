package com.jf.PetApp.core.security;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor for utility class
    }

    /**
     * The {@code app_context} the current session's JWT carries (see {@link AppContextEnum}),
     * read back from the {@code APP_CONTEXT_*} granted authority {@code JwtAuthenticationFilter}
     * stamps onto the {@code Authentication}. Empty for a token minted before this claim existed
     * or one that requested no context — callers that need to tell a Wallet session apart from
     * an Academy one (e.g. Mentor, which both apps share) must treat that as "unknown," never
     * default it to either context.
     */
    public static Optional<AppContextEnum> getCurrentAppContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(AppContextEnum::fromAuthority)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
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
