package com.cps.mfa.mfa;

import com.cps.mfa.common.ApiException;
import com.cps.mfa.common.HashUtil;
import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Core OTP engine shared by the email and SMS channels and by step-up MFA. Enforces the OTP
 * security policy: 6 digits, single-use, TTL, max attempts and a resend cooldown. Only the
 * BCrypt hash of the code is persisted.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    /** Generates, hashes and stores a fresh OTP, returning the plaintext to hand to the channel. */
    public String create(User user, MfaMethodType type, Long stepUpActionId) {
        enforceResendCooldown(user, type, stepUpActionId);

        String plain = HashUtil.numericOtp(properties.getOtp().getLength());
        OtpCode code = new OtpCode();
        code.setUser(user);
        code.setOtpType(type);
        code.setStepUpActionId(stepUpActionId);
        code.setOtpHash(passwordEncoder.encode(plain));
        code.setExpiresAt(Instant.now().plus(Duration.ofMinutes(properties.getOtp().getTtlMinutes())));
        repository.save(code);
        return plain;
    }

    /**
     * Verifies a submitted OTP. Returns true on success. Returns false on a wrong code while
     * attempts remain. Throws on expiry, exhausted attempts, or when no challenge exists.
     */
    public boolean verify(User user, MfaMethodType type, Long stepUpActionId, String submitted) {
        OtpCode code = (stepUpActionId == null
                ? repository.findFirstByUserAndOtpTypeAndUsedFalseAndStepUpActionIdIsNullOrderByCreatedAtDesc(user, type)
                : repository.findFirstByUserAndStepUpActionIdAndUsedFalseOrderByCreatedAtDesc(user, stepUpActionId))
                .orElseThrow(() -> ApiException.badRequest("No active OTP challenge. Request a new code."));

        if (code.getExpiresAt().isBefore(Instant.now())) {
            code.setUsed(true);
            repository.save(code);
            throw ApiException.badRequest("OTP expired. Request a new code.");
        }
        if (code.getAttemptCount() >= properties.getOtp().getMaxAttempts()) {
            code.setUsed(true);
            repository.save(code);
            throw ApiException.tooManyRequests("Maximum OTP attempts exceeded. Request a new code.");
        }

        code.setAttemptCount(code.getAttemptCount() + 1);

        if (passwordEncoder.matches(submitted, code.getOtpHash())) {
            code.setUsed(true); // single-use
            repository.save(code);
            return true;
        }
        repository.save(code);
        return false;
    }

    private void enforceResendCooldown(User user, MfaMethodType type, Long stepUpActionId) {
        Optional<OtpCode> last = stepUpActionId == null
                ? repository.findFirstByUserAndOtpTypeAndUsedFalseAndStepUpActionIdIsNullOrderByCreatedAtDesc(user, type)
                : repository.findFirstByUserAndStepUpActionIdAndUsedFalseOrderByCreatedAtDesc(user, stepUpActionId);
        last.ifPresent(otp -> {
            Instant earliest = otp.getCreatedAt()
                    .plus(Duration.ofSeconds(properties.getOtp().getResendCooldownSeconds()));
            if (earliest.isAfter(Instant.now())) {
                throw ApiException.tooManyRequests("Please wait before requesting another code.");
            }
        });
    }
}
