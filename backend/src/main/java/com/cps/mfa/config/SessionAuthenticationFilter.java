package com.cps.mfa.config;

import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.session.AuthPrincipal;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the opaque Bearer session token into a Spring Security principal.
 *
 * <p>Enforces two gateway rules at the edge:
 * <ol>
 *   <li>Timed-out sessions are expired and rejected.</li>
 *   <li>PENDING_MFA sessions (password done, MFA not yet) may ONLY reach MFA/auth endpoints —
 *       they cannot touch HMI, admin or any protected resource until MFA promotes them.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            Optional<UserSession> maybe = sessionService.findByRawToken(token);
            if (maybe.isPresent()) {
                UserSession session = maybe.get();

                if (session.getStatus() == SessionStatus.ACTIVE && sessionService.isTimedOut(session)) {
                    sessionService.expire(session, "Session timed out (idle/absolute)");
                    reject(response, "SESSION_EXPIRED", "Your session has expired. Please log in again.");
                    return;
                }

                boolean usable = session.getStatus() == SessionStatus.ACTIVE
                        || session.getStatus() == SessionStatus.PENDING_MFA;
                if (usable) {
                    if (session.getStatus() == SessionStatus.PENDING_MFA && !isPreAuthPath(request)) {
                        reject(response, "MFA_REQUIRED", "Complete multi-factor authentication first.");
                        return;
                    }
                    authenticate(session);
                    // Refresh the idle timer only on meaningful interaction. Passive polling
                    // (status/session/me) must NOT keep an otherwise-idle session alive, otherwise
                    // the idle-timeout behaviour could never be observed.
                    if (session.getStatus() == SessionStatus.ACTIVE && !isPassivePath(request)) {
                        sessionService.touch(session);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(UserSession session) {
        User user = session.getUser();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (session.getStatus() == SessionStatus.PENDING_MFA) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PRE_AUTH"));
        } else {
            user.getRoles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName())));
            user.permissionNames().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        AuthPrincipal principal = new AuthPrincipal(user, session);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** Read-only polling endpoints that should not reset the idle timer. */
    private boolean isPassivePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/hmi/status")
                || path.equals("/api/session/current")
                || path.equals("/api/auth/me");
    }

    /** Endpoints a half-authenticated (PENDING_MFA) session is permitted to call. */
    private boolean isPreAuthPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/mfa/")
                || path.equals("/api/auth/me")
                || path.equals("/api/auth/logout");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void reject(HttpServletResponse response, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
