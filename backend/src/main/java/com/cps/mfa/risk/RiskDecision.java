package com.cps.mfa.risk;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RiskLevel;

import java.util.List;

/**
 * Outcome of a risk evaluation: the numeric score, the band, whether the login is blocked,
 * the MFA method the user must satisfy, and a human-readable breakdown for the UI/audit trail.
 */
public record RiskDecision(
        int score,
        RiskLevel level,
        boolean blocked,
        MfaMethodType requiredMethod,
        List<RiskReason> reasons
) {
}
