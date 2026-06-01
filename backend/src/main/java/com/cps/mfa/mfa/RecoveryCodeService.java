package com.cps.mfa.mfa;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.HashUtil;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.config.AppProperties;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Backup recovery codes: generated in bulk, shown once, stored hashed, each usable a single time. */
@Service
@RequiredArgsConstructor
public class RecoveryCodeService {

    private final RecoveryCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties properties;

    /** Regenerates the full set, invalidating any previous codes. Returns the plaintext codes ONCE. */
    @Transactional
    public List<String> regenerate(User user) {
        repository.deleteByUser(user);
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < properties.getRecovery().getCodeCount(); i++) {
            String code = HashUtil.recoveryCode();
            plain.add(code);
            RecoveryCode entity = new RecoveryCode();
            entity.setUser(user);
            entity.setCodeHash(passwordEncoder.encode(code));
            repository.save(entity);
        }
        auditService.log(AuditEventType.RECOVERY_CODES_REGENERATED, Severity.MEDIUM, user, null, null,
                "Generated " + plain.size() + " backup recovery codes");
        return plain;
    }

    /** Verifies and consumes a recovery code. Each successful use is audited. */
    @Transactional
    public boolean verify(User user, String submitted, RequestMeta meta) {
        List<RecoveryCode> codes = repository.findByUserAndUsedFalse(user);
        String normalized = submitted == null ? "" : submitted.trim().toUpperCase();
        for (RecoveryCode code : codes) {
            if (passwordEncoder.matches(normalized, code.getCodeHash())) {
                code.setUsed(true);
                code.setUsedAt(Instant.now());
                repository.save(code);
                auditService.log(AuditEventType.RECOVERY_CODE_USED, Severity.MEDIUM, user, meta, null,
                        "Backup recovery code used (" + remaining(user) + " remaining)");
                return true;
            }
        }
        return false;
    }

    public long remaining(User user) {
        return repository.findByUserAndUsedFalse(user).size();
    }
}
