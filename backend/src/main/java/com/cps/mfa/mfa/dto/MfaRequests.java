package com.cps.mfa.mfa.dto;

import jakarta.validation.constraints.NotBlank;

/** Request payloads for the MFA endpoints, grouped for brevity. */
public final class MfaRequests {

    private MfaRequests() {
    }

    public record OtpVerifyRequest(@NotBlank String code) {
    }

    public record RecoveryVerifyRequest(@NotBlank String code) {
    }

    public record WebAuthnFinishRequest(String credentialId, String publicKey, String authenticatorName) {
    }
}
