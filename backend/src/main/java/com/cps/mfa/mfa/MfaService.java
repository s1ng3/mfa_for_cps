package com.cps.mfa.mfa;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.*;
import com.cps.mfa.incident.IncidentService;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.risk.TrustedDevice;
import com.cps.mfa.risk.TrustedDeviceRepository;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;
import com.cps.mfa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Coordinates the MFA stage for a PENDING_MFA session: sending challenges, verifying the second
 * factor, promoting the session to ACTIVE on success, and escalating repeated failures to an incident.
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final int MAX_FAILED_MFA = 3;

    private final EmailOtpService emailOtpService;
    private final SmsOtpService smsOtpService;
    private final OtpService otpService;
    private final WebAuthnService webAuthnService;
    private final RecoveryCodeService recoveryCodeService;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IncidentService incidentService;
    private final UserRepository userRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;

    // ---- Challenge dispatch ---------------------------------------------------------------

    public void sendEmailOtp(UserSession session, RequestMeta meta) {
        emailOtpService.send(session.getUser(), null, meta);
    }

    public void sendSmsOtp(UserSession session, RequestMeta meta) {
        smsOtpService.send(session.getUser(), null, meta);
    }

    // ---- Verification ---------------------------------------------------------------------

    @Transactional(noRollbackFor = ApiException.class)
    public boolean verifyOtp(UserSession session, MfaMethodType type, String code, RequestMeta meta) {
        boolean ok = otpService.verify(session.getUser(), type, null, code);
        if (ok) {
            completeLogin(session, type, meta);
        } else {
            handleFailedMfa(session, meta, type.name());
        }
        return ok;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public boolean verifyWebAuthn(UserSession session, String credentialId, RequestMeta meta) {
        boolean ok = webAuthnService.finishAuthentication(session.getUser(), credentialId, meta);
        if (ok) {
            completeLogin(session, MfaMethodType.WEBAUTHN, meta);
        } else {
            handleFailedMfa(session, meta, "WEBAUTHN");
        }
        return ok;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public boolean verifyRecoveryCode(UserSession session, String code, RequestMeta meta) {
        boolean ok = recoveryCodeService.verify(session.getUser(), code, meta);
        if (ok) {
            completeLogin(session, MfaMethodType.RECOVERY_CODE, meta);
        } else {
            handleFailedMfa(session, meta, "RECOVERY_CODE");
        }
        return ok;
    }

    /** Shared success path: reset counters, trust the device, audit, and activate the session. */
    private void completeLogin(UserSession session, MfaMethodType method, RequestMeta meta) {
        User user = session.getUser();
        user.setFailedMfaAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.log(AuditEventType.MFA_SUCCESS, Severity.LOW, user, meta, session.getRiskScore(),
                "MFA verified via " + method);

        rememberDevice(user, meta);
        sessionService.activate(session, meta);
    }

    private void handleFailedMfa(UserSession session, RequestMeta meta, String methodLabel) {
        User user = session.getUser();
        user.setFailedMfaAttempts(user.getFailedMfaAttempts() + 1);
        userRepository.save(user);

        auditService.log(AuditEventType.MFA_FAILED, Severity.MEDIUM, user, meta, session.getRiskScore(),
                "Failed " + methodLabel + " verification (attempt " + user.getFailedMfaAttempts() + ")");
        notificationService.notify("FAILED_MFA", Severity.MEDIUM, user,
                "Failed MFA for " + user.getUsername() + " (attempt " + user.getFailedMfaAttempts() + ")");

        if (user.getFailedMfaAttempts() >= MAX_FAILED_MFA) {
            incidentService.create(Severity.HIGH, "Repeated MFA failures",
                    user.getUsername() + " failed MFA " + user.getFailedMfaAttempts() + " times.", user);
            sessionService.terminate(session, "Blocked after repeated MFA failures");
            user.setFailedMfaAttempts(0);
            userRepository.save(user);
            throw ApiException.tooManyRequests("Too many failed MFA attempts. Please log in again.");
        }
        throw ApiException.badRequest("Invalid code. "
                + (MAX_FAILED_MFA - user.getFailedMfaAttempts()) + " attempt(s) remaining.");
    }

    /** Adds/refreshes the device fingerprint as trusted so future logins score lower. */
    private void rememberDevice(User user, RequestMeta meta) {
        TrustedDevice device = trustedDeviceRepository
                .findByUserAndDeviceFingerprint(user, meta.deviceFingerprint())
                .orElseGet(() -> {
                    TrustedDevice d = new TrustedDevice();
                    d.setUser(user);
                    d.setDeviceFingerprint(meta.deviceFingerprint());
                    d.setDeviceName(meta.userAgent());
                    return d;
                });
        device.setIpAddress(meta.ipAddress());
        device.setLastSeenAt(Instant.now());
        trustedDeviceRepository.save(device);
    }
}
