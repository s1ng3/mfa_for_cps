package com.cps.mfa.mfa;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.HashUtil;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.config.WebAuthnConfig;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebAuthn/FIDO2 service.
 *
 * <p><b>MOCK implementation.</b> The registration and authentication <i>ceremonies</i> are real in
 * shape — challenges are issued and tracked, credentials are persisted, signature counters advance —
 * but the cryptographic attestation/assertion verification is stubbed. To make this production-grade,
 * replace the body of {@link #finishRegistration} / {@link #finishAuthentication} with a real library
 * such as Yubico's {@code java-webauthn-server}; the persistence model ({@link WebAuthnCredential})
 * and endpoints stay the same.</p>
 *
 * <p>Biometric login (Windows Hello / Touch ID) is the same WebAuthn flow with a platform
 * authenticator, so it is covered here too.</p>
 */
@Service
@RequiredArgsConstructor
public class WebAuthnService {

    private final WebAuthnCredentialRepository credentialRepository;
    private final AuditService auditService;
    private final WebAuthnConfig config;

    /** Outstanding challenges keyed by username (an in-memory store is fine for a single-node demo). */
    private final Map<String, String> challenges = new ConcurrentHashMap<>();

    // ---- Registration ceremony ------------------------------------------------------------

    public Map<String, Object> startRegistration(User user) {
        String challenge = HashUtil.randomToken();
        challenges.put("reg:" + user.getUsername(), challenge);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", challenge);
        options.put("rp", Map.of("id", config.getRpId(), "name", config.getRpName()));
        options.put("user", Map.of(
                "id", String.valueOf(user.getId()),
                "name", user.getUsername(),
                "displayName", user.getUsername()));
        options.put("pubKeyCredParams", List.of(Map.of("type", "public-key", "alg", -7)));
        options.put("timeout", 60000);
        options.put("attestation", "none");
        options.put("authenticatorSelection", Map.of(
                "userVerification", "preferred",
                "residentKey", "preferred"));
        return options;
    }

    public void finishRegistration(User user, String credentialId, String publicKey,
                                   String authenticatorName, RequestMeta meta) {
        String expected = challenges.remove("reg:" + user.getUsername());
        if (expected == null) {
            auditService.log(AuditEventType.WEBAUTHN_FAILED, Severity.MEDIUM, user, meta, null,
                    "Registration finish with no active challenge");
            throw com.cps.mfa.common.ApiException.badRequest("No active registration challenge.");
        }
        // MOCK: a real RP would verify the attestation object & client data against `expected`.
        WebAuthnCredential credential = new WebAuthnCredential();
        credential.setUser(user);
        credential.setCredentialId(credentialId != null ? credentialId : "mock-" + HashUtil.randomToken());
        credential.setPublicKey(publicKey != null ? publicKey : "MOCK_PUBLIC_KEY");
        credential.setAuthenticatorName(authenticatorName != null ? authenticatorName : "Platform authenticator");
        credentialRepository.save(credential);

        auditService.log(AuditEventType.WEBAUTHN_REGISTERED, Severity.LOW, user, meta, null,
                "Registered authenticator: " + credential.getAuthenticatorName());
    }

    // ---- Authentication ceremony ----------------------------------------------------------

    public Map<String, Object> startAuthentication(User user) {
        String challenge = HashUtil.randomToken();
        challenges.put("auth:" + user.getUsername(), challenge);

        List<Map<String, String>> allow = credentialRepository.findByUser(user).stream()
                .map(c -> Map.of("type", "public-key", "id", c.getCredentialId()))
                .toList();

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", challenge);
        options.put("rpId", config.getRpId());
        options.put("timeout", 60000);
        options.put("userVerification", "preferred");
        options.put("allowCredentials", allow);
        // Signals to the frontend whether a real credential exists or this is a pure mock flow.
        options.put("mock", allow.isEmpty());
        return options;
    }

    /**
     * Verifies an assertion. Returns true on success. In MOCK mode (no stored credential) it
     * accepts the ceremony so the demo flow completes; otherwise it advances the signature counter.
     */
    public boolean finishAuthentication(User user, String credentialId, RequestMeta meta) {
        String expected = challenges.remove("auth:" + user.getUsername());
        if (expected == null) {
            auditService.log(AuditEventType.WEBAUTHN_FAILED, Severity.MEDIUM, user, meta, null,
                    "Authentication finish with no active challenge");
            return false;
        }

        var credentials = credentialRepository.findByUser(user);
        if (credentials.isEmpty()) {
            // MOCK: user has no enrolled key yet — accept for the demo and note it in the trail.
            auditService.log(AuditEventType.WEBAUTHN_SUCCESS, Severity.LOW, user, meta, null,
                    "WebAuthn verified (MOCK: no enrolled credential)");
            return true;
        }

        WebAuthnCredential credential = credentialId != null
                ? credentialRepository.findByCredentialId(credentialId).orElse(credentials.get(0))
                : credentials.get(0);
        // MOCK: a real RP would verify the assertion signature with the stored public key here.
        credential.setSignatureCount(credential.getSignatureCount() + 1);
        credential.setLastUsedAt(Instant.now());
        credentialRepository.save(credential);

        auditService.log(AuditEventType.WEBAUTHN_SUCCESS, Severity.LOW, user, meta, null,
                "WebAuthn assertion verified for " + credential.getAuthenticatorName());
        return true;
    }
}
