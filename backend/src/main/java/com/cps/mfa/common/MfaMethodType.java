package com.cps.mfa.common;

/** Supported second-factor methods. BIOMETRIC is realised through WebAuthn / Windows Hello. */
public enum MfaMethodType {
    EMAIL_OTP,
    SMS_OTP,
    WEBAUTHN,
    BIOMETRIC,
    RECOVERY_CODE
}
