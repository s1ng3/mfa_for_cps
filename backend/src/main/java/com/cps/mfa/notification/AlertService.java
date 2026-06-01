package com.cps.mfa.notification;

import com.cps.mfa.common.Severity;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Persists security alerts for display in the admin dashboard. */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SecurityAlertRepository repository;

    public SecurityAlert create(String alertType, Severity severity, User user, String message) {
        SecurityAlert alert = new SecurityAlert();
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setMessage(message);
        if (user != null) {
            alert.setUserId(user.getId());
            alert.setUsername(user.getUsername());
        }
        return repository.save(alert);
    }

    public List<SecurityAlert> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    public long unreadCount() {
        return repository.countByReadStatusFalse();
    }
}
