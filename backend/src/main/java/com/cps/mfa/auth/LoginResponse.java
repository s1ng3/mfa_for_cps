package com.cps.mfa.auth;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RiskLevel;
import com.cps.mfa.risk.RiskReason;

import java.util.List;
import java.util.Set;

/**
 * Result of the password stage. If {@code blocked} is true the login was refused at CRITICAL risk.
 * Otherwise {@code mfaToken} is a PENDING_MFA session token the client uses to complete {@code requiredMethod}.
 */
public record LoginResponse(
        boolean blocked,
        String mfaToken,
        String username,
        Set<String> roles,
        MfaMethodType requiredMethod,
        int riskScore,
        RiskLevel riskLevel,
        List<RiskReason> riskReasons,
        String message
) {
}
