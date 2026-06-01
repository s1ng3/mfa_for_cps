package com.cps.mfa.notification;

import com.cps.mfa.common.Severity;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * High-level notification facade. Persists an in-app {@link SecurityAlert} (shown in the admin
 * dashboard) and fans the message out to the mocked email/SMS channels for HIGH/CRITICAL events.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AlertService alertService;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;

    public void notify(String alertType, Severity severity, User user, String message) {
        alertService.create(alertType, severity, user, message);

        // Only escalate the noisier channels for serious events.
        if (severity == Severity.HIGH || severity == Severity.CRITICAL) {
            String to = user != null ? user.getEmail() : "soc@cps.local";
            emailNotificationService.send(to, "[CPS-MFA] " + alertType, message);
            if (user != null && user.getPhoneNumber() != null) {
                smsNotificationService.send(user.getPhoneNumber(), alertType + ": " + message);
            }
        }
    }
}
