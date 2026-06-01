package com.cps.mfa.common;

/** Account state. LOCKED accounts cannot authenticate until an admin unlocks them. */
public enum AccountStatus {
    ACTIVE, LOCKED, DISABLED
}
