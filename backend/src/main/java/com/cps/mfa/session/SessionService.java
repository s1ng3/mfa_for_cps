package com.cps.mfa.session;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.HashUtil;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.rbac.RoleName;
import com.cps.mfa.risk.RiskDecision;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Owns the session lifecycle: issuing pending sessions at login, promoting them to ACTIVE after
 * MFA, tracking activity, enforcing timeouts and terminating sessions.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository repository;
    private final AuditService auditService;
    private final AppProperties properties;

    /** Raw token + persisted session. The raw token is only ever returned here, never stored. */
    public record IssuedSession(String rawToken, UserSession session) {
    }

    /** Creates a PENDING_MFA session immediately after a valid password, before MFA. */
    public IssuedSession createPending(User user, RequestMeta meta, RiskDecision decision) {
        String rawToken = HashUtil.randomToken();
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionTokenHash(HashUtil.sha256(rawToken));
        session.setIpAddress(meta.ipAddress());
        session.setDeviceFingerprint(meta.deviceFingerprint());
        session.setUserAgent(meta.userAgent());
        session.setRiskScore(decision.score());
        session.setRequiredMfaMethod(decision.requiredMethod());
        session.setStatus(SessionStatus.PENDING_MFA);
        session.setExpiresAt(Instant.now().plus(Duration.ofMinutes(absoluteTimeout(user))));
        return new IssuedSession(rawToken, repository.save(session));
    }

    /** Promotes a session to ACTIVE once MFA succeeds and logs SESSION_CREATED. */
    public UserSession activate(UserSession session, RequestMeta meta) {
        session.setStatus(SessionStatus.ACTIVE);
        session.setLastActivityAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(Duration.ofMinutes(absoluteTimeout(session.getUser()))));
        UserSession saved = repository.save(session);
        auditService.log(AuditEventType.SESSION_CREATED, Severity.LOW, session.getUser(), meta,
                session.getRiskScore(), "Session activated after successful MFA");
        return saved;
    }

    public Optional<UserSession> findByRawToken(String rawToken) {
        return repository.findBySessionTokenHash(HashUtil.sha256(rawToken));
    }

    public void touch(UserSession session) {
        session.setLastActivityAt(Instant.now());
        repository.save(session);
    }

    public void markStepUpVerified(UserSession session) {
        session.setStepUpValidUntil(Instant.now()
                .plus(Duration.ofSeconds(properties.getSession().getStepUpValiditySeconds())));
        repository.save(session);
    }

    public void terminate(UserSession session, String reason) {
        session.setStatus(SessionStatus.TERMINATED);
        session.setTerminatedAt(Instant.now());
        repository.save(session);
        auditService.log(AuditEventType.SESSION_TERMINATED, Severity.MEDIUM, session.getUser(), null,
                session.getRiskScore(), reason);
    }

    public void expire(UserSession session, String reason) {
        session.setStatus(SessionStatus.EXPIRED);
        repository.save(session);
        auditService.log(AuditEventType.SESSION_EXPIRED, Severity.LOW, session.getUser(), null,
                session.getRiskScore(), reason);
    }

    public List<UserSession> findActiveLike() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public UserSession findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> com.cps.mfa.common.ApiException.notFound("Session not found: " + id));
    }

    /** Absolute lifetime in minutes — admins get a shorter window. */
    public int absoluteTimeout(User user) {
        return user.hasRole(RoleName.ADMIN)
                ? properties.getSession().getAdminTimeoutMinutes()
                : properties.getSession().getAbsoluteTimeoutMinutes();
    }

    /** True if the session has exceeded its idle or absolute deadline. */
    public boolean isTimedOut(UserSession session) {
        Instant now = Instant.now();
        boolean idle = session.getLastActivityAt() != null && session.getLastActivityAt()
                .plus(Duration.ofMinutes(properties.getSession().getIdleTimeoutMinutes())).isBefore(now);
        boolean absolute = session.getExpiresAt() != null && session.getExpiresAt().isBefore(now);
        return idle || absolute;
    }
}
