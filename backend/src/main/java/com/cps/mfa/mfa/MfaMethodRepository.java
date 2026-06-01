package com.cps.mfa.mfa;

import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MfaMethodRepository extends JpaRepository<MfaMethod, Long> {
    List<MfaMethod> findByUser(User user);
}
