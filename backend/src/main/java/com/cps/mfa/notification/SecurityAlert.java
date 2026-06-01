package com.cps.mfa.notification;

import com.cps.mfa.common.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A surfaced alert shown in the admin dashboard (and "sent" via mocked email/SMS). */
@Entity
@Table(name = "security_alerts")
@Getter
@Setter
@NoArgsConstructor
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 60)
    private String username;

    @Column(nullable = false, length = 60)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.MEDIUM;

    @Column(length = 1000)
    private String message;

    private boolean readStatus = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
