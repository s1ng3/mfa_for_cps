package com.cps.mfa.rbac;

/** Canonical role names. Kept as constants so controllers/services avoid magic strings. */
public final class RoleName {
    public static final String VIEWER = "VIEWER";
    public static final String OPERATOR = "OPERATOR";
    public static final String ENGINEER = "ENGINEER";
    public static final String ADMIN = "ADMIN";
    public static final String SECURITY_OFFICER = "SECURITY_OFFICER";

    private RoleName() {
    }
}
