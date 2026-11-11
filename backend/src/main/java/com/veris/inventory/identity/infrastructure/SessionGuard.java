package com.veris.inventory.identity.infrastructure;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import com.veris.inventory.identity.application.LocalProfiles;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

public final class SessionGuard extends OncePerRequestFilter {
    public static final String PROFILE = SessionGuard.class.getName() + ".profile";
    public static final String START = SessionGuard.class.getName() + ".start";
    private final LocalProfiles profiles;
    private final Duration lifetime;
    public SessionGuard(LocalProfiles profiles, Duration lifetime) { this.profiles = profiles; this.lifetime = lifetime; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            var session = request.getSession(false);
            var start = session == null ? null : (Instant) session.getAttribute(START);
            if (start == null || !Instant.now().isBefore(start.plus(lifetime))) {
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                SecurityEvents.record("SESSION_EXPIRED", "unknown");
                SecurityEvents.problem(response, 401, "SESSION_EXPIRED");
                return;
            }
            try {
                request.setAttribute(PROFILE, profiles.require(user));
            } catch (OAuth2AuthenticationException denied) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                SecurityEvents.record("PROFILE_ACCESS_DENIED", "unknown");
                SecurityEvents.problem(response, 403, "ACCESS_DENIED");
                return;
            } catch (org.springframework.dao.DataAccessException unavailable) {
                SecurityEvents.problem(response, 503, "IDENTITY_STORE_UNAVAILABLE");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
