package com.cps.mfa.mfa;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One-time password challenge. Only the BCrypt hash of the code is stored; codes are
 * single-use, expire after a TTL and allow a bounded number of verification attempts.
 */
@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaMethodType otpType;

    /** Marks this OTP as a step-up challenge tied to a specific critical action. */
    private Long stepUpActionId;

    @Column(nullable = false)
    private Instant expiresAt;

    private boolean used = false;
    private int attemptCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
