package com.cps.mfa.session;

import com.cps.mfa.user.User;

/** The authenticated principal carried in the Spring Security context: the user + their session. */
public record AuthPrincipal(User user, UserSession session) {
}
