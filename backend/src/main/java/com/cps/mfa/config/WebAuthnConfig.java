package com.cps.mfa.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Relying-Party parameters for WebAuthn/FIDO2. In this demo the ceremony is mocked, but these
 * values are shaped exactly as a real RP (e.g. with the {@code java-webauthn-server} library) needs.
 */
@Component
@Getter
public class WebAuthnConfig {
    /** Relying Party ID — normally the registrable domain (here, localhost for the dev demo). */
    private final String rpId = "localhost";
    private final String rpName = "CPS MFA Security Gateway";
    private final String origin = "http://localhost:5173";
}
