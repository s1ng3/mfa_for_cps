package com.cps.mfa.common;

/** Minimal success/message envelope for endpoints that have no richer payload. */
public record SimpleResponse(boolean success, String message) {
    public static SimpleResponse ok(String message) {
        return new SimpleResponse(true, message);
    }
}
