package com.cps.mfa.mfa;

import com.cps.mfa.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A registered WebAuthn/FIDO2 authenticator (security key or platform authenticator such as
 * Windows Hello biometrics). In this demo the publicKey/credentialId are populated by a mock
 * ceremony, but the shape matches a real WebAuthn relying-party store so it can be swapped in.
 */
@Entity
@Table(name = "webauthn_credentials")
@Getter
@Setter
@NoArgsConstructor
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 256)
    private String credentialId;

    @Column(length = 1024)
    private String publicKey;

    private long signatureCount = 0;

    @Column(length = 120)
    private String authenticatorName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant lastUsedAt;
}
