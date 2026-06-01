package com.cps.mfa.config;

import com.cps.mfa.common.MfaMethodType;
import com.cps.mfa.mfa.MfaMethod;
import com.cps.mfa.mfa.MfaMethodRepository;
import com.cps.mfa.mfa.WebAuthnCredential;
import com.cps.mfa.mfa.WebAuthnCredentialRepository;
import com.cps.mfa.rbac.*;
import com.cps.mfa.user.User;
import com.cps.mfa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Seeds permissions, roles and the five demo users on first boot. Idempotent: if any users
 * already exist, seeding is skipped. Default password for every demo user is {@code Password123!}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MfaMethodRepository mfaMethodRepository;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed data already present — skipping seeding.");
            return;
        }
        log.info("Seeding RBAC model and demo users...");

        seedPermissions();
        Role viewer = role(RoleName.VIEWER, "Read-only HMI access", Permissions.HMI_VIEW);
        Role operator = role(RoleName.OPERATOR, "Operate the process",
                Permissions.HMI_VIEW, Permissions.HMI_START_PUMP, Permissions.HMI_STOP_PUMP,
                Permissions.HMI_ACK_ALARM);
        Role engineer = role(RoleName.ENGINEER, "Operator + critical setpoint changes (step-up MFA)",
                Permissions.HMI_VIEW, Permissions.HMI_START_PUMP, Permissions.HMI_STOP_PUMP,
                Permissions.HMI_ACK_ALARM, Permissions.HMI_CHANGE_MOTOR_SPEED,
                Permissions.HMI_CHANGE_PRESSURE, Permissions.HMI_CHANGE_TEMPERATURE,
                Permissions.HMI_RESET_ESTOP);
        Role admin = role(RoleName.ADMIN, "User, session, audit and incident administration",
                Permissions.ADMIN_MANAGE_USERS, Permissions.ADMIN_VIEW_SESSIONS,
                Permissions.ADMIN_TERMINATE_SESSIONS, Permissions.AUDIT_VIEW,
                Permissions.INCIDENT_VIEW, Permissions.INCIDENT_MANAGE);
        Role security = role(RoleName.SECURITY_OFFICER, "Audit, incident investigation and SIEM export",
                Permissions.AUDIT_VIEW, Permissions.INCIDENT_VIEW, Permissions.INCIDENT_MANAGE,
                Permissions.SIEM_EXPORT);

        user("viewer1", "viewer1@cps.local", "+40700000001", viewer,
                MfaMethodType.EMAIL_OTP);
        user("operator1", "operator1@cps.local", "+40700000002", operator,
                MfaMethodType.EMAIL_OTP, MfaMethodType.SMS_OTP);
        User engineer1 = user("engineer1", "engineer1@cps.local", "+40700000003", engineer,
                MfaMethodType.EMAIL_OTP, MfaMethodType.SMS_OTP, MfaMethodType.WEBAUTHN);
        User admin1 = user("admin1", "admin1@cps.local", "+40700000004", admin,
                MfaMethodType.WEBAUTHN, MfaMethodType.EMAIL_OTP);
        user("security1", "security1@cps.local", "+40700000005", security,
                MfaMethodType.EMAIL_OTP, MfaMethodType.SMS_OTP);

        // Pre-enrol a mock WebAuthn credential for the WebAuthn-using accounts so the demo can
        // exercise the "verify against a stored credential" path (not just the empty-mock path).
        mockCredential(engineer1, "Engineer YubiKey 5 (mock)");
        mockCredential(admin1, "Admin Windows Hello (mock)");

        log.info("Seeding complete. Demo users (password '{}'): viewer1, operator1, engineer1, admin1, security1",
                DEMO_PASSWORD);
    }

    private void seedPermissions() {
        String[] all = {
                Permissions.HMI_VIEW, Permissions.HMI_START_PUMP, Permissions.HMI_STOP_PUMP,
                Permissions.HMI_ACK_ALARM, Permissions.HMI_CHANGE_MOTOR_SPEED,
                Permissions.HMI_CHANGE_PRESSURE, Permissions.HMI_CHANGE_TEMPERATURE,
                Permissions.HMI_RESET_ESTOP, Permissions.ADMIN_MANAGE_USERS,
                Permissions.ADMIN_VIEW_SESSIONS, Permissions.ADMIN_TERMINATE_SESSIONS,
                Permissions.AUDIT_VIEW, Permissions.INCIDENT_VIEW, Permissions.INCIDENT_MANAGE,
                Permissions.SIEM_EXPORT
        };
        for (String name : all) {
            permissionRepository.findByName(name)
                    .orElseGet(() -> permissionRepository.save(new Permission(name, name)));
        }
    }

    private Role role(String name, String description, String... permissionNames) {
        Role role = new Role(name, description);
        Set<Permission> perms = new LinkedHashSet<>();
        Arrays.stream(permissionNames).forEach(p ->
                permissionRepository.findByName(p).ifPresent(perms::add));
        role.setPermissions(perms);
        return roleRepository.save(role);
    }

    private User user(String username, String email, String phone, Role role, MfaMethodType... methods) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRoles(Set.of(role));
        User saved = userRepository.save(user);
        for (MfaMethodType method : methods) {
            mfaMethodRepository.save(new MfaMethod(saved, method));
        }
        return saved;
    }

    private void mockCredential(User user, String name) {
        WebAuthnCredential credential = new WebAuthnCredential();
        credential.setUser(user);
        credential.setCredentialId("seed-cred-" + user.getUsername());
        credential.setPublicKey("MOCK_SEED_PUBLIC_KEY");
        credential.setAuthenticatorName(name);
        webAuthnCredentialRepository.save(credential);
    }
}
