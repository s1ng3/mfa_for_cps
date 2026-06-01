package com.cps.mfa.mfa;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Records which second factors a user has enrolled and verified. */
@Entity
@Table(name = "mfa_methods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "methodType"}))
@Getter
@Setter
@NoArgsConstructor
public class MfaMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaMethodType methodType;

    private boolean enabled = true;
    private boolean verified = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant lastUsedAt;

    public MfaMethod(User user, MfaMethodType methodType) {
        this.user = user;
        this.methodType = methodType;
    }
}
