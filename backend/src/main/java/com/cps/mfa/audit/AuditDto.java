package com.cps.mfa.audit;

import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.Severity;

import java.time.Instant;

/** Read model for audit log views and SIEM export. */
public record AuditDto(
        Long id,
        Long userId,
        String username,
        AuditEventType eventType,
        Severity severity,
        String ipAddress,
        String deviceFingerprint,
        Integer riskScore,
        String details,
        Instant createdAt
) {
    public static AuditDto from(AuditLog l) {
        return new AuditDto(l.getId(), l.getUserId(), l.getUsername(), l.getEventType(),
                l.getSeverity(), l.getIpAddress(), l.getDeviceFingerprint(), l.getRiskScore(),
                l.getDetails(), l.getCreatedAt());
    }
}
