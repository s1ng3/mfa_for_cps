package com.cps.mfa.common;

/**
 * Lifecycle of a {@code UserSession}.
 * <ul>
 *   <li>PENDING_MFA – password accepted, awaiting second factor (token only valid on MFA endpoints)</li>
 *   <li>ACTIVE – fully authenticated</li>
 *   <li>EXPIRED – idle/absolute timeout</li>
 *   <li>TERMINATED – killed by admin or logout</li>
 *   <li>BLOCKED – never elevated (e.g. CRITICAL risk login)</li>
 * </ul>
 */
public enum SessionStatus {
    PENDING_MFA, ACTIVE, EXPIRED, TERMINATED, BLOCKED
}
