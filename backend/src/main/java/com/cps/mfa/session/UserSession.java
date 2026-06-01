package com.cps.mfa.session;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A login session. The raw session token is never stored; only its SHA-256 hash is persisted
 * so the gateway can validate, track and terminate sessions without holding the secret.
 */
@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 128)
    private String sessionTokenHash;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 128)
    private String deviceFingerprint;

    @Column(length = 255)
    private String userAgent;

    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.PENDING_MFA;

    /** MFA method demanded for this session before it can become ACTIVE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MfaMethodType requiredMfaMethod;

    /** Until this instant, step-up MFA is considered satisfied for critical actions. */
    private Instant stepUpValidUntil;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant lastActivityAt = Instant.now();
    private Instant expiresAt;
    private Instant terminatedAt;

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean stepUpValid() {
        return stepUpValidUntil != null && stepUpValidUntil.isAfter(Instant.now());
    }
}
