package com.cps.mfa.session;

import com.cps.mfa.common.ApiException;
import com.cps.mfa.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static accessor for the currently authenticated principal in the request scope. */
public final class AuthContext {

    private AuthContext() {
    }

    public static AuthPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw ApiException.unauthorized("No authenticated session");
        }
        return principal;
    }

    public static User currentUser() {
        return current().user();
    }

    public static UserSession currentSession() {
        return current().session();
    }
}
