package com.cps.mfa.session;

import com.cps.mfa.common.SessionStatus;

import java.time.Instant;

/** Read model for session listings (admin dashboard + current-session view). */
public record SessionDto(
        Long id,
        String username,
        String ipAddress,
        String deviceFingerprint,
        String userAgent,
        int riskScore,
        SessionStatus status,
        boolean stepUpValid,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        Instant terminatedAt
) {
    public static SessionDto from(UserSession s) {
        return new SessionDto(s.getId(), s.getUser().getUsername(), s.getIpAddress(),
                s.getDeviceFingerprint(), s.getUserAgent(), s.getRiskScore(), s.getStatus(),
                s.stepUpValid(), s.getCreatedAt(), s.getLastActivityAt(), s.getExpiresAt(),
                s.getTerminatedAt());
    }
}
