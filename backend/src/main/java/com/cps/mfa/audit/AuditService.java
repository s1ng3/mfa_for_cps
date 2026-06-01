package com.cps.mfa.audit;

import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single entry point for writing audit events. Keeping all writes here guarantees a uniform
 * record shape and makes the SIEM exporter trivial.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;

    public AuditLog log(AuditEventType eventType, Severity severity, User user,
                        RequestMeta meta, Integer riskScore, String details) {
        AuditLog entry = new AuditLog();
        entry.setEventType(eventType);
        entry.setSeverity(severity);
        if (user != null) {
            entry.setUserId(user.getId());
            entry.setUsername(user.getUsername());
        }
        if (meta != null) {
            entry.setIpAddress(meta.ipAddress());
            entry.setDeviceFingerprint(meta.deviceFingerprint());
        }
        entry.setRiskScore(riskScore);
        entry.setDetails(details);
        AuditLog saved = repository.save(entry);
        log.info("AUDIT [{}] severity={} user={} ip={} risk={} :: {}",
                eventType, severity, entry.getUsername(), entry.getIpAddress(), riskScore, details);
        return saved;
    }

    /** Convenience overload for events with no associated user (e.g. unknown-username login). */
    public AuditLog log(AuditEventType eventType, Severity severity, String username,
                        RequestMeta meta, String details) {
        AuditLog entry = new AuditLog();
        entry.setEventType(eventType);
        entry.setSeverity(severity);
        entry.setUsername(username);
        if (meta != null) {
            entry.setIpAddress(meta.ipAddress());
            entry.setDeviceFingerprint(meta.deviceFingerprint());
        }
        entry.setDetails(details);
        AuditLog saved = repository.save(entry);
        log.info("AUDIT [{}] severity={} user={} :: {}", eventType, severity, username, details);
        return saved;
    }
}
