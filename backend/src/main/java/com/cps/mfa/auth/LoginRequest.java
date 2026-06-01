package com.cps.mfa.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials plus optional demo-only simulation flags that let a presenter deliberately
 * raise the risk score (new device / unknown IP / off-hours) without changing machines.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        boolean simulateNewDevice,
        boolean simulateUnknownIp,
        boolean simulateOutsideHours
) {
}
