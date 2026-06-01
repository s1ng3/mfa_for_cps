package com.cps.mfa.mfa;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.rbac.RoleName;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Encapsulates MFA policy decisions that are independent of a specific login's risk score. */
@Service
@RequiredArgsConstructor
public class MfaPolicyService {

    private final MfaMethodRepository mfaMethodRepository;

    /** ADMIN (and any privileged operator) must always use a phishing-resistant strong factor. */
    public boolean requiresStrongMfa(User user) {
        return user.hasRole(RoleName.ADMIN);
    }

    public boolean isStrong(MfaMethodType method) {
        return method == MfaMethodType.WEBAUTHN || method == MfaMethodType.BIOMETRIC;
    }

    /** Methods the user has enrolled — surfaced to the UI so it can offer fallbacks. */
    public List<MfaMethodType> enrolledMethods(User user) {
        return mfaMethodRepository.findByUser(user).stream()
                .filter(MfaMethod::isEnabled)
                .map(MfaMethod::getMethodType)
                .toList();
    }
}
