package com.cps.mfa.risk;

import com.cps.mfa.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A device fingerprint the risk engine treats as known/trusted for a user. */
@Entity
@Table(name = "trusted_devices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "deviceFingerprint"}))
@Getter
@Setter
@NoArgsConstructor
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(length = 120)
    private String deviceName;

    @Column(length = 64)
    private String ipAddress;

    private boolean trusted = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant lastSeenAt = Instant.now();
}
