package com.cps.mfa.risk;

/**
 * Inputs to the risk engine that come from the login attempt itself. The {@code simulate*}
 * flags let the demo deliberately drive up the score (new device / unknown IP / off-hours)
 * without having to physically change machines or wait for night-time.
 */
public record RiskContext(
        boolean simulateNewDevice,
        boolean simulateUnknownIp,
        boolean simulateOutsideHours
) {
    public static RiskContext none() {
        return new RiskContext(false, false, false);
    }
}
