package com.jf.PetApp.infrastructure.security.jwt;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
        TokenProvider tokenProvider,
        UserRepository userRepository
    ) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (tokenProvider.validate(token)) {
                String email = tokenProvider.extractSubject(token);
                Optional<AppContextEnum> appContext = tokenProvider.extractAppContext(token);

                userRepository.findByEmail(email).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication =
                        buildAuthentication(user, appContext.orElse(null));

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    // appContext is null for a token minted before this claim existed, or with no app_context
    // requested at login — such a session gets no APP_CONTEXT_* authority, so it can still reach
    // shared endpoints but not the ones scoped to a specific app (see SecurityConfig).
    private UsernamePasswordAuthenticationToken buildAuthentication(User user, AppContextEnum appContext) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (appContext != null) {
            authorities.add(new SimpleGrantedAuthority(appContext.authority()));
        }

        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }
}