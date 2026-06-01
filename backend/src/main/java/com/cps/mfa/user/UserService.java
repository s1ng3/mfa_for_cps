package com.cps.mfa.user;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.auth.PasswordService;
import com.cps.mfa.common.AccountStatus;
import com.cps.mfa.common.ApiException;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.common.Severity;
import com.cps.mfa.rbac.Role;
import com.cps.mfa.rbac.RoleRepository;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.session.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** User administration: listing, creation and lock/unlock (with session revocation on lock). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final AuditService auditService;
    private final SessionService sessionService;
    private final UserSessionRepository sessionRepository;

    public List<UserDto> list() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.badRequest("Username already exists: " + request.username());
        }
        Set<Role> roles = request.roles().stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> ApiException.badRequest("Unknown role: " + name)))
                .collect(Collectors.toSet());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setRoles(roles);
        User saved = userRepository.save(user);

        auditService.log(AuditEventType.ACCOUNT_UNLOCKED, Severity.LOW, saved, null, null,
                "User created with roles " + request.roles());
        return UserDto.from(saved);
    }

    @Transactional
    public UserDto lock(Long id) {
        User user = find(id);
        user.setAccountStatus(AccountStatus.LOCKED);
        userRepository.save(user);
        auditService.log(AuditEventType.ACCOUNT_LOCKED, Severity.HIGH, user, null, null,
                "Account locked by administrator");

        // Revoke any live sessions immediately.
        for (UserSession session : sessionRepository.findByUserAndStatus(user, SessionStatus.ACTIVE)) {
            sessionService.terminate(session, "User account locked by administrator");
        }
        return UserDto.from(user);
    }

    @Transactional
    public UserDto unlock(Long id) {
        User user = find(id);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setFailedMfaAttempts(0);
        userRepository.save(user);
        auditService.log(AuditEventType.ACCOUNT_UNLOCKED, Severity.MEDIUM, user, null, null,
                "Account unlocked by administrator");
        return UserDto.from(user);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }
}
