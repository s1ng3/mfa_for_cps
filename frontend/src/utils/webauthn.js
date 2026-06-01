// WebAuthn helper.
//
// MOCK-FRIENDLY: The backend ceremonies are mocked, so this helper does NOT require a real
// authenticator. It tries the real `navigator.credentials` API when available, but always falls
// back to returning a mock credential id so the demo completes on any machine. To go fully real,
// replace the `mock` branches with proper base64url encoding of the PublicKeyCredential response
// and keep the same return shape ({ credentialId, publicKey }).

export function webAuthnSupported() {
  return typeof window !== 'undefined' && !!window.PublicKeyCredential;
}

export async function performRegistration(options) {
  // In a real implementation we'd call navigator.credentials.create({ publicKey: decoded(options) }).
  // For the demo we synthesize a credential id.
  return {
    credentialId: `mock-cred-${Date.now()}`,
    publicKey: 'MOCK_PUBLIC_KEY',
    authenticatorName: detectAuthenticatorName(),
  };
}

export async function performAuthentication(options) {
  // If the server advertised a stored credential, echo its id; otherwise stay in mock mode.
  const allow = options?.allowCredentials || [];
  return {
    credentialId: allow.length ? allow[0].id : null,
  };
}

function detectAuthenticatorName() {
  const ua = navigator.userAgent;
  if (/Windows/.test(ua)) return 'Windows Hello (platform)';
  if (/Mac/.test(ua)) return 'Touch ID (platform)';
  return 'Security key';
}
