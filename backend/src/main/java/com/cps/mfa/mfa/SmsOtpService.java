package com.cps.mfa.mfa;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.notification.SmsNotificationService;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Sends SMS OTP challenges. The code is delivered via the mock SMS sink (backend console). */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOtpService {

    private final OtpService otpService;
    private final SmsNotificationService smsNotificationService;
    private final AuditService auditService;
    private final AppProperties properties;

    public void send(User user, Long stepUpActionId, RequestMeta meta) {
        String code = otpService.create(user, MfaMethodType.SMS_OTP, stepUpActionId);
        String to = user.getPhoneNumber() != null ? user.getPhoneNumber() : "<no phone on file>";
        smsNotificationService.send(to, "CPS-MFA code: " + code);
        if (properties.getMfa().isMockPrintOtp()) {
            log.info(">>> [DEMO] SMS OTP for {} = {}", user.getUsername(), code);
        }
        auditService.log(AuditEventType.MFA_SMS_SENT, Severity.LOW, user, meta, null,
                "SMS OTP sent to " + to);
    }
}
