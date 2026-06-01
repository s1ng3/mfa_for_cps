package com.cps.mfa.rbac;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A fine-grained capability (e.g. {@code HMI_START_PUMP}). Roles aggregate permissions;
 * the {@link AuthorizationService} checks them and logs unauthorized attempts.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
