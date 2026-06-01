package com.cps.mfa.hmi;

import com.cps.mfa.common.CpsActionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** An audit-grade record of a CPS control action, including whether step-up MFA was required. */
@Entity
@Table(name = "cps_actions")
@Getter
@Setter
@NoArgsConstructor
public class CpsAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 60)
    private String username;

    @Column(nullable = false, length = 60)
    private String actionType;

    @Column(length = 60)
    private String targetComponent;

    @Column(length = 60)
    private String oldValue;

    @Column(length = 60)
    private String newValue;

    private boolean requiresStepUp;
    private boolean stepUpVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CpsActionStatus status = CpsActionStatus.PENDING_STEP_UP;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
