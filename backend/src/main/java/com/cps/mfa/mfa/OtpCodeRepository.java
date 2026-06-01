package com.cps.mfa.mfa;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /** Most recent unused OTP of a given type for a user (login MFA, stepUpActionId == null). */
    Optional<OtpCode> findFirstByUserAndOtpTypeAndUsedFalseAndStepUpActionIdIsNullOrderByCreatedAtDesc(
            User user, MfaMethodType otpType);

    Optional<OtpCode> findFirstByUserAndStepUpActionIdAndUsedFalseOrderByCreatedAtDesc(
            User user, Long stepUpActionId);

    List<OtpCode> findByUserAndUsedFalse(User user);
}
