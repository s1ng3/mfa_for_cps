package com.cps.mfa.hmi;

/** Outcome of an HMI action attempt: either executed, or step-up MFA is required first. */
public record CpsActionResult(
        boolean executed,
        boolean requiresStepUp,
        Long actionId,
        String message,
        HmiStatus status
) {
}
