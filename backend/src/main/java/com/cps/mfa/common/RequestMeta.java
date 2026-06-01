package com.cps.mfa.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/** Per-request client metadata (IP, device fingerprint, user-agent) extracted from the HTTP request. */
public record RequestMeta(String ipAddress, String deviceFingerprint, String userAgent) {

    public static RequestMeta from(HttpServletRequest request) {
        String ip = header(request, "X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }

        // The frontend computes a stable device fingerprint and sends it on every request.
        String fp = header(request, "X-Device-Fingerprint");
        if (fp == null) {
            fp = "unknown-" + UUID.nameUUIDFromBytes((ip == null ? "" : ip).getBytes());
        }

        String ua = header(request, "User-Agent");
        return new RequestMeta(ip, fp, ua);
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? null : value;
    }
}
