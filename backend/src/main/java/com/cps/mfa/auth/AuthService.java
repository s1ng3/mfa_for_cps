package com.cps.mfa.auth;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.*;
import com.cps.mfa.incident.IncidentService;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.risk.RiskContext;
import com.cps.mfa.risk.RiskDecision;
import com.cps.mfa.risk.RiskEngine;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;
import com.cps.mfa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the password stage of the gateway: credential check, lockout, risk evaluation,
 * CRITICAL-risk blocking, and issuing a PENDING_MFA session that the MFA stage will promote.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGINS = 5;

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final RiskEngine riskEngine;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IncidentService incidentService;

    // noRollbackFor: failed-login lockouts / audit rows must persist even though we throw afterwards.
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest request, RequestMeta meta) {
        User user = userRepository.findByUsername(request.username()).orElse(null);

        // Do not reveal whether the username exists.
        if (user == null) {
            auditService.log(AuditEventType.AUTH_LOGIN_FAILED, Severity.MEDIUM, request.username(), meta,
                    "Login attempt for unknown username");
            throw ApiException.unauthorized("Invalid username or password");
        }

        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            auditService.log(AuditEventType.AUTH_LOGIN_FAILED, Severity.HIGH, user, meta, null,
                    "Login attempt on LOCKED account");
            throw ApiException.locked("Account is locked. Contact an administrator.");
        }
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw ApiException.forbidden("Account is disabled.");
        }

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            handleFailedPassword(user, meta);
            throw ApiException.unauthorized("Invalid username or password");
        }

        // Password verified. Evaluate risk using the CURRENT failure counters, then reset logins.
        RiskContext ctx = new RiskContext(
                request.simulateNewDevice(), request.simulateUnknownIp(), request.simulateOutsideHours());
        RiskDecision decision = riskEngine.evaluate(user, meta, ctx);

        auditService.log(AuditEventType.AUTH_LOGIN_SUCCESS, Severity.LOW, user, meta, decision.score(),
                "Password verified; risk=" + decision.level() + " (" + decision.score() + "); MFA="
                        + decision.requiredMethod());

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        raiseRiskNotifications(user, decision, ctx);

        if (decision.blocked()) {
            notificationService.notify("HIGH_RISK_LOGIN_BLOCKED", Severity.CRITICAL, user,
                    "CRITICAL risk login blocked for " + user.getUsername() + " (score " + decision.score() + ")");
            incidentService.create(Severity.CRITICAL, "Critical-risk login blocked",
                    "Login for " + user.getUsername() + " scored " + decision.score()
                            + " and was blocked by the risk engine.", user);
            return new LoginResponse(true, null, user.getUsername(), user.roleNames(),
                    decision.requiredMethod(), decision.score(), decision.level(), decision.reasons(),
                    "Login blocked due to critical risk. An administrator has been notified.");
        }

        SessionService.IssuedSession issued = sessionService.createPending(user, meta, decision);
        return new LoginResponse(false, issued.rawToken(), user.getUsername(), user.roleNames(),
                decision.requiredMethod(), decision.score(), decision.level(), decision.reasons(),
                "Credentials accepted. Complete " + decision.requiredMethod() + " to continue.");
    }

    private void handleFailedPassword(User user, RequestMeta meta) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        auditService.log(AuditEventType.AUTH_LOGIN_FAILED, Severity.MEDIUM, user, meta, null,
                "Incorrect password (attempt " + user.getFailedLoginAttempts() + ")");
        notificationService.notify("FAILED_LOGIN", Severity.MEDIUM, user,
                "Failed login for " + user.getUsername() + " (attempt " + user.getFailedLoginAttempts() + ")");

        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGINS) {
            user.setAccountStatus(AccountStatus.LOCKED);
            auditService.log(AuditEventType.ACCOUNT_LOCKED, Severity.HIGH, user, meta, null,
                    "Account locked after " + user.getFailedLoginAttempts() + " failed logins");
            notificationService.notify("ACCOUNT_LOCKED", Severity.HIGH, user,
                    user.getUsername() + " locked after repeated failed logins");
            incidentService.create(Severity.HIGH, "Account locked: repeated failed logins",
                    user.getUsername() + " reached " + MAX_FAILED_LOGINS + " failed login attempts.", user);
        }
        userRepository.save(user);
    }

    private void raiseRiskNotifications(User user, RiskDecision decision, RiskContext ctx) {
        if (decision.level() == RiskLevel.HIGH) {
            notificationService.notify("HIGH_RISK_LOGIN", Severity.HIGH, user,
                    "High-risk login for " + user.getUsername() + " (score " + decision.score() + ")");
        }
        boolean newDevice = decision.reasons().stream()
                .anyMatch(r -> r.rule().startsWith("New"));
        if (newDevice) {
            notificationService.notify("UNKNOWN_DEVICE_LOGIN", Severity.MEDIUM, user,
                    "Login from a new/unrecognised device for " + user.getUsername());
        }
    }

    @Transactional
    public void logout(UserSession session) {
        sessionService.terminate(session, "User logout");
    }
}
