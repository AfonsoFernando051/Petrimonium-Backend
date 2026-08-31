package com.jf.PetApp.infrastructure.security.jwt;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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

                userRepository.findByEmail(email).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication =
                        buildAuthentication(user);

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
            user,
            null,
            List.of(new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().name()
            ))
        );
    }
}