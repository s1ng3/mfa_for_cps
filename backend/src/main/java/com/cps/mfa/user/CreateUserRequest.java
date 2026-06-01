package com.cps.mfa.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Payload for admin-driven user creation. */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotEmpty Set<String> roles
) {
}
