package com.cps.mfa.mfa;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.notification.EmailNotificationService;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Sends Email OTP challenges. The code is delivered via the mock email sink (backend console). */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private final OtpService otpService;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;
    private final AppProperties properties;

    public void send(User user, Long stepUpActionId, RequestMeta meta) {
        String code = otpService.create(user, MfaMethodType.EMAIL_OTP, stepUpActionId);
        emailNotificationService.send(user.getEmail(), "Your CPS-MFA verification code",
                "Your one-time code is: " + code + " (valid for "
                        + properties.getOtp().getTtlMinutes() + " minutes)");
        if (properties.getMfa().isMockPrintOtp()) {
            // DEMO ONLY: echo the OTP so graders can read it without an inbox.
            log.info(">>> [DEMO] EMAIL OTP for {} = {}", user.getUsername(), code);
        }
        auditService.log(AuditEventType.MFA_EMAIL_SENT, Severity.LOW, user, meta, null,
                "Email OTP sent to " + user.getEmail());
    }
}
