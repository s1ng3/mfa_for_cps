import client from '../api/client';

export const mfaApi = {
  sendEmail: () => client.post('/mfa/email/send').then((r) => r.data),
  verifyEmail: (code) => client.post('/mfa/email/verify', { code }).then((r) => r.data),
  sendSms: () => client.post('/mfa/sms/send').then((r) => r.data),
  verifySms: (code) => client.post('/mfa/sms/verify', { code }).then((r) => r.data),

  webauthnAuthStart: () => client.post('/mfa/webauthn/authenticate/start').then((r) => r.data),
  webauthnAuthFinish: (payload) => client.post('/mfa/webauthn/authenticate/finish', payload).then((r) => r.data),
  webauthnRegStart: () => client.post('/mfa/webauthn/register/start').then((r) => r.data),
  webauthnRegFinish: (payload) => client.post('/mfa/webauthn/register/finish', payload).then((r) => r.data),

  verifyRecovery: (code) => client.post('/mfa/recovery/verify', { code }).then((r) => r.data),
  regenerateRecovery: () => client.post('/mfa/recovery/regenerate').then((r) => r.data),
};
