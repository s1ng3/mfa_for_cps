package com.cps.mfa.admin;

import com.cps.mfa.audit.AuditDto;
import com.cps.mfa.incident.Incident;
import com.cps.mfa.notification.SecurityAlert;

import java.util.List;

/** Aggregated security posture for the admin dashboard landing view. */
public record DashboardDto(
        long totalUsers,
        long activeSessions,
        long openIncidents,
        long unreadAlerts,
        List<SecurityAlert> recentAlerts,
        List<Incident> recentIncidents,
        List<AuditDto> recentEvents
) {
}
