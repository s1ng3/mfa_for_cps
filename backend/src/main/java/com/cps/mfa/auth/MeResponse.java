package com.cps.mfa.auth;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;

import java.time.Instant;
import java.util.Set;

/** Profile + session state for the authenticated (or half-authenticated) caller. */
public record MeResponse(
        Long userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        SessionStatus sessionStatus,
        boolean mfaPending,
        MfaMethodType requiredMfaMethod,
        int riskScore,
        boolean stepUpValid,
        Instant sessionExpiresAt
) {
    public static MeResponse from(User user, UserSession session) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.roleNames(),
                user.permissionNames(),
                session.getStatus(),
                session.getStatus() == SessionStatus.PENDING_MFA,
                session.getRequiredMfaMethod(),
                session.getRiskScore(),
                session.stepUpValid(),
                session.getExpiresAt());
    }
}
