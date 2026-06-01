export const methodLabel = {
  EMAIL_OTP: 'Email OTP',
  SMS_OTP: 'SMS OTP',
  WEBAUTHN: 'WebAuthn / FIDO2',
  BIOMETRIC: 'Biometric (Windows Hello)',
  RECOVERY_CODE: 'Backup recovery code',
};

// Maps a required MFA method to the route that collects it.
export const methodRoute = {
  EMAIL_OTP: '/mfa/email',
  SMS_OTP: '/mfa/sms',
  WEBAUTHN: '/mfa/webauthn',
  BIOMETRIC: '/mfa/webauthn',
};

export function riskColor(level) {
  return { LOW: 'var(--low)', MEDIUM: 'var(--medium)', HIGH: 'var(--high)', CRITICAL: 'var(--critical)' }[level] || 'var(--muted)';
}
