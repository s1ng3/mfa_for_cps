package com.cps.mfa.risk;

import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    Optional<TrustedDevice> findByUserAndDeviceFingerprint(User user, String deviceFingerprint);
}
