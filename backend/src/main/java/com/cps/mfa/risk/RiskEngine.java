package com.cps.mfa.risk;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.RiskLevel;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.rbac.RoleName;
import com.cps.mfa.session.UserSessionRepository;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptive risk engine. Aggregates weighted rules into a 0–100 score, maps the score to a band,
 * and selects the required MFA strength. ADMIN logins always escalate to strong MFA (WebAuthn).
 *
 * <pre>
 *   0–30  LOW      → password + Email OTP
 *  31–60  MEDIUM   → password + SMS OTP
 *  61–80  HIGH     → password + WebAuthn/FIDO2
 *  81–100 CRITICAL → block login, notify admin, create incident
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserSessionRepository sessionRepository;
    private final AppProperties properties;

    public RiskDecision evaluate(User user, RequestMeta meta, RiskContext ctx) {
        List<RiskReason> reasons = new ArrayList<>();

        boolean newDevice = ctx.simulateNewDevice()
                || trustedDeviceRepository.findByUserAndDeviceFingerprint(user, meta.deviceFingerprint()).isEmpty();
        if (newDevice) {
            reasons.add(new RiskReason("New / unrecognised device", 25));
        }
        if (ctx.simulateUnknownIp()) {
            reasons.add(new RiskReason("Unknown IP address", 25));
        }
        if (ctx.simulateOutsideHours() || isOutsideWorkingHours()) {
            reasons.add(new RiskReason("Login outside working hours", 15));
        }
        if (user.hasRole(RoleName.ADMIN)) {
            reasons.add(new RiskReason("Privileged ADMIN role", 20));
        } else if (user.hasRole(RoleName.ENGINEER)) {
            reasons.add(new RiskReason("ENGINEER role", 10));
        }
        if (user.getFailedLoginAttempts() > 0) {
            reasons.add(new RiskReason("Recent failed password attempts", 20));
        }
        if (user.getFailedMfaAttempts() > 0) {
            reasons.add(new RiskReason("Recent failed MFA attempts", 30));
        }
        long activeSessions = sessionRepository.countByUserAndStatus(user, SessionStatus.ACTIVE);
        if (activeSessions >= 1) {
            reasons.add(new RiskReason("Multiple concurrent active sessions", 20));
        }

        int score = Math.min(100, reasons.stream().mapToInt(RiskReason::points).sum());
        RiskLevel level = bandFor(score);
        boolean blocked = level == RiskLevel.CRITICAL;
        MfaMethodType method = selectMethod(user, level);

        return new RiskDecision(score, level, blocked, method, reasons);
    }

    private RiskLevel bandFor(int score) {
        if (score <= 30) return RiskLevel.LOW;
        if (score <= 60) return RiskLevel.MEDIUM;
        if (score <= 80) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    private MfaMethodType selectMethod(User user, RiskLevel level) {
        // Policy override: privileged admins must always use strong (WebAuthn/FIDO2) MFA.
        if (user.hasRole(RoleName.ADMIN)) {
            return MfaMethodType.WEBAUTHN;
        }
        return switch (level) {
            case LOW -> MfaMethodType.EMAIL_OTP;
            case MEDIUM -> MfaMethodType.SMS_OTP;
            case HIGH -> MfaMethodType.WEBAUTHN;
            case CRITICAL -> MfaMethodType.WEBAUTHN; // login is blocked anyway
        };
    }

    private boolean isOutsideWorkingHours() {
        LocalTime now = LocalTime.now();
        return now.getHour() < properties.getWorkingHours().getStart()
                || now.getHour() >= properties.getWorkingHours().getEnd();
    }
}
