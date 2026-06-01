package com.cps.mfa.mfa;

import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findByUser(User user);

    Optional<WebAuthnCredential> findByCredentialId(String credentialId);

    boolean existsByUser(User user);
}
