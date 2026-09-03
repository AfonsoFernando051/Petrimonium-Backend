package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.infrastructure.security.RequestIdFilter;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.jf.PetApp.infrastructure.security.ratelimit.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

// Enables @PreAuthorize/@PostAuthorize/hasRole(...) on individual use cases or controller
// methods. No endpoint uses it yet — the ADMIN role in RoleEnum has no admin-only
// functionality behind it today (verified: no route in this codebase should currently be
// admin-restricted). This annotation is forward-compatible groundwork only, so that the day
// an admin-only endpoint is actually added, protecting it is a one-line @PreAuthorize instead
// of first having to remember to wire up method security at all.
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    // Comma-separated list of allowed origins, environment-specific. Blank by default — the
    // mobile app doesn't need CORS at all (native HTTP client, not a browser), so an
    // unconfigured environment gets no cross-origin access rather than silently defaulting to
    // permissive. application-dev.properties sets the wildcard for local browser tooling (H2
    // console etc.); application-prod.properties requires a real origin with no fallback.
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    // Same flag that gates the H2 console itself (application.properties vs
    // application-prod.properties) — the only reason X-Frame-Options was ever disabled was to
    // let the H2 console render in an iframe, so it's disabled only where that console is
    // actually enabled. Everywhere else (including prod) keeps the safe same-origin default.
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        TokenProvider tokenProvider,
        UserRepository userRepository
    ) {
        return new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(
        @Value("${app.security.trusted-proxies:}") String trustedProxies
    ) {
        return new RateLimitingFilter(trustedProxies);
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RateLimitingFilter rateLimitingFilter,
        RequestIdFilter requestIdFilter
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .headers(headers -> headers
                .frameOptions(frame -> {
                    if (h2ConsoleEnabled) {
                        frame.disable();
                    } else {
                        frame.sameOrigin();
                    }
                })
                // A pure JSON API never renders untrusted HTML, so the strictest possible
                // policy is safe — unlike HSTS/X-Frame-Options, Spring Security has no CSP
                // default, so it has to be set explicitly or the header is simply absent.
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // BFF enforcement (docs/BACKEND_MODULE_PLAN.md §5, ECOSYSTEM.md): real_portfolio
                // is real money and must never answer an Academy-context session; education
                // content is Academy-only. The canonical Pet/XP summary (/api/pets, /api/v1/
                // gamification) is intentionally shared — Stage 6 (Pet/XP/Mentor context
                // separation) confirmed the pet is meant to be a single cross-app companion, and
                // its XP is already restricted by an allow-list (XpEventType: only
                // LESSON_COMPLETED/MODULE_COMPLETED/SIMULATOR_COMPLETED feed it — see
                // AchievementCatalog's DECISION-014/DECISION-027 comments) to never include
                // wealth/profit signals, so a Wallet session seeing Academy-earned XP there is by
                // design, not a leak.
                .requestMatchers("/api/investments/**").hasAuthority(AppContextEnum.WALLET.authority())
                .requestMatchers("/api/v1/academy/**", "/api/v1/learning/**", "/api/v1/lab/**",
                        "/api/v1/simulated-portfolios/**", "/api/v1/missions/**")
                    .hasAuthority(AppContextEnum.ACADEMY.authority())
                // Achievement badges are evaluated against the real portfolio (AchievementCatalog
                // is wealth-threshold-based, e.g. portfolio_10k) — Wallet-only, same reasoning as
                // /api/investments/**, so an Academy session (which has no real portfolio to
                // speak of) never triggers that evaluation.
                .requestMatchers("/api/v1/achievements/**").hasAuthority(AppContextEnum.WALLET.authority())
                // Health is the user's real cash flow — accounts, salary, bills, card invoices.
                // Strictly its own context: an Academy session (simulated money) must never read
                // it, and neither must a Wallet one, which answers a different question
                // (patrimony) and has no reason to see day-to-day spending. HealthService
                // additionally derives the owner from the JWT subject on every call, so this
                // rule is the outer gate, not the only one.
                .requestMatchers("/api/v1/health/**").hasAuthority(AppContextEnum.HEALTH.authority())
                // Mentor is shared but context-*sensitive*: GetMentorReplyUseCaseImpl builds a
                // different system prompt (real portfolio vs simulated + Academy progress)
                // depending on which app the session belongs to, so a session with no resolvable
                // app_context can't safely be served — require one of the two rather than
                // falling through to bare `authenticated()`.
                .requestMatchers("/api/mentor/**")
                    .hasAnyAuthority(AppContextEnum.WALLET.authority(), AppContextEnum.ACADEMY.authority())
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            // Explicitly ordered ahead of the JWT filter (not just both "before
            // UsernamePasswordAuthenticationFilter", which wouldn't fix their relative order) —
            // a rate-limited request should never reach JWT parsing at all.
            .addFilterBefore(
                rateLimitingFilter,
                JwtAuthenticationFilter.class
            )
            // Runs first of all three: every request, including rate-limited/rejected ones,
            // should carry a correlation id in its logs and response.
            .addFilterBefore(
                requestIdFilter,
                RateLimitingFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        // An unconfigured/blank value now means "no cross-origin access" (empty list), not a
        // silent wildcard — see the allowedOrigins field doc above for why.
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
