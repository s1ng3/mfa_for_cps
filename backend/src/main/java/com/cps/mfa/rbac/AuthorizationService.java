package com.cps.mfa.rbac;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.ApiException;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.incident.IncidentService;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central RBAC enforcement point. A denied check is a security event: it is audited, raises an
 * alert, and — after repeated attempts by the same user — auto-creates an incident.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final int UNAUTHORIZED_INCIDENT_THRESHOLD = 3;

    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IncidentService incidentService;

    private final ConcurrentHashMap<String, AtomicInteger> unauthorizedCounts = new ConcurrentHashMap<>();

    public boolean hasPermission(User user, String permission) {
        return user.permissionNames().contains(permission);
    }

    /** Throws 403 (and records the attempt) if the user lacks the permission. */
    public void require(User user, String permission, RequestMeta meta, String actionDescription) {
        if (hasPermission(user, permission)) {
            return;
        }

        auditService.log(AuditEventType.UNAUTHORIZED_ACTION_ATTEMPT, Severity.HIGH, user, meta, null,
                "Denied '" + actionDescription + "' (missing permission " + permission + ")");
        notificationService.notify("UNAUTHORIZED_ACTION_ATTEMPT", Severity.HIGH, user,
                user.getUsername() + " attempted unauthorized action: " + actionDescription);

        int count = unauthorizedCounts
                .computeIfAbsent(user.getUsername(), k -> new AtomicInteger())
                .incrementAndGet();
        if (count >= UNAUTHORIZED_INCIDENT_THRESHOLD) {
            incidentService.create(Severity.HIGH, "Repeated unauthorized HMI access attempts",
                    user.getUsername() + " has made " + count + " unauthorized action attempts.", user);
            unauthorizedCounts.get(user.getUsername()).set(0);
        }

        throw ApiException.forbidden("You are not authorized to perform: " + actionDescription);
    }
}
