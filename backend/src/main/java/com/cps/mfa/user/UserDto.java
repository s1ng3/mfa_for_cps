package com.cps.mfa.user;

import com.cps.mfa.common.AccountStatus;

import java.time.Instant;
import java.util.Set;

/** Read model for user administration screens. Never exposes the password hash. */
public record UserDto(
        Long id,
        String username,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        Set<String> roles,
        int failedLoginAttempts,
        int failedMfaAttempts,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getPhoneNumber(),
                u.getAccountStatus(), u.roleNames(), u.getFailedLoginAttempts(),
                u.getFailedMfaAttempts(), u.getLastLoginAt(), u.getCreatedAt());
    }
}
