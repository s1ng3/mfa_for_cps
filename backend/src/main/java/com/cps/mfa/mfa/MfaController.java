package com.cps.mfa.mfa;

import com.cps.mfa.auth.MeResponse;
import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.SimpleResponse;
import com.cps.mfa.mfa.dto.MfaRequests.*;
import com.cps.mfa.session.AuthContext;
import com.cps.mfa.session.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MFA stage endpoints. The caller is identified by their PENDING_MFA (or ACTIVE) session token.
 * Successful login-stage verification promotes the session to ACTIVE and returns the fresh profile.
 */
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;
    private final WebAuthnService webAuthnService;
    private final RecoveryCodeService recoveryCodeService;

    // ---- Email OTP ------------------------------------------------------------------------

    @PostMapping("/email/send")
    public SimpleResponse sendEmail(HttpServletRequest http) {
        mfaService.sendEmailOtp(AuthContext.currentSession(), RequestMeta.from(http));
        return SimpleResponse.ok("Email OTP sent (check the mocked email / backend console).");
    }

    @PostMapping("/email/verify")
    public MeResponse verifyEmail(@Valid @RequestBody OtpVerifyRequest req, HttpServletRequest http) {
        mfaService.verifyOtp(AuthContext.currentSession(), MfaMethodType.EMAIL_OTP, req.code(), RequestMeta.from(http));
        return profile();
    }

    // ---- SMS OTP --------------------------------------------------------------------------

    @PostMapping("/sms/send")
    public SimpleResponse sendSms(HttpServletRequest http) {
        mfaService.sendSmsOtp(AuthContext.currentSession(), RequestMeta.from(http));
        return SimpleResponse.ok("SMS OTP sent (check the mocked SMS / backend console).");
    }

    @PostMapping("/sms/verify")
    public MeResponse verifySms(@Valid @RequestBody OtpVerifyRequest req, HttpServletRequest http) {
        mfaService.verifyOtp(AuthContext.currentSession(), MfaMethodType.SMS_OTP, req.code(), RequestMeta.from(http));
        return profile();
    }

    // ---- WebAuthn / FIDO2 / biometric -----------------------------------------------------

    @PostMapping("/webauthn/register/start")
    public Map<String, Object> registerStart() {
        return webAuthnService.startRegistration(AuthContext.currentUser());
    }

    @PostMapping("/webauthn/register/finish")
    public SimpleResponse registerFinish(@RequestBody WebAuthnFinishRequest req, HttpServletRequest http) {
        webAuthnService.finishRegistration(AuthContext.currentUser(), req.credentialId(), req.publicKey(),
                req.authenticatorName(), RequestMeta.from(http));
        return SimpleResponse.ok("Authenticator registered.");
    }

    @PostMapping("/webauthn/authenticate/start")
    public Map<String, Object> authenticateStart() {
        return webAuthnService.startAuthentication(AuthContext.currentUser());
    }

    @PostMapping("/webauthn/authenticate/finish")
    public MeResponse authenticateFinish(@RequestBody WebAuthnFinishRequest req, HttpServletRequest http) {
        mfaService.verifyWebAuthn(AuthContext.currentSession(), req.credentialId(), RequestMeta.from(http));
        return profile();
    }

    // ---- Backup recovery codes ------------------------------------------------------------

    @PostMapping("/recovery/verify")
    public MeResponse verifyRecovery(@Valid @RequestBody RecoveryVerifyRequest req, HttpServletRequest http) {
        mfaService.verifyRecoveryCode(AuthContext.currentSession(), req.code(), RequestMeta.from(http));
        return profile();
    }

    @PostMapping("/recovery/regenerate")
    public Map<String, Object> regenerateRecovery() {
        var codes = recoveryCodeService.regenerate(AuthContext.currentUser());
        // Codes are returned exactly once; the client must show and discard them.
        return Map.of("codes", codes, "count", codes.size(),
                "warning", "Store these now — they will not be shown again.");
    }

    private MeResponse profile() {
        AuthPrincipal principal = AuthContext.current();
        return MeResponse.from(principal.user(), principal.session());
    }
}
