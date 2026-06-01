package com.cps.mfa.mfa;

import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
    List<RecoveryCode> findByUserAndUsedFalse(User user);

    void deleteByUser(User user);
}
