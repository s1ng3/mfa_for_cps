import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { landingPath } from '../utils/nav';
import { mfaApi } from '../services/mfaApi';
import { apiError } from '../api/client';
import { performAuthentication, performRegistration, webAuthnSupported } from '../utils/webauthn';

export default function WebAuthnPage() {
  const { completeMfa } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  const authenticate = async () => {
    setBusy(true); setError(null); setInfo('Starting WebAuthn ceremony…');
    try {
      const options = await mfaApi.webauthnAuthStart();
      const assertion = await performAuthentication(options);
      if (options.mock) setInfo('No physical key enrolled — completing mock ceremony.');
      const me = await mfaApi.webauthnAuthFinish({ credentialId: assertion.credentialId });
      completeMfa(me);
      navigate(landingPath(me), { replace: true });
    } catch (e) { setError(apiError(e)); }
    finally { setBusy(false); }
  };

  const register = async () => {
    setBusy(true); setError(null); setInfo(null);
    try {
      const options = await mfaApi.webauthnRegStart();
      const cred = await performRegistration(options);
      await mfaApi.webauthnRegFinish(cred);
      setInfo('Authenticator registered. You can now verify with it.');
    } catch (e) { setError(apiError(e)); }
    finally { setBusy(false); }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>WebAuthn / FIDO2</h1>
        <div className="auth-sub">
          Phishing-resistant strong authentication (security key or platform biometrics such as
          Windows Hello). {webAuthnSupported() ? '' : 'This browser reports no WebAuthn support — the demo uses a mock ceremony.'}
        </div>

        <div className="alert-box info">
          This is a <strong>structurally-real but mocked</strong> WebAuthn flow: challenges and
          credentials are tracked server-side; cryptographic verification is stubbed for the demo.
        </div>

        <button className="btn" disabled={busy} onClick={authenticate}>
          {busy ? 'Working…' : '🔑 Verify with security key / biometrics'}
        </button>
        <button className="btn secondary" disabled={busy} onClick={register}>
          Register a new authenticator
        </button>

        {info && <div className="alert-box ok">{info}</div>}
        {error && <div className="alert-box error">{error}</div>}
        <button className="btn secondary mt" onClick={() => navigate('/mfa')}>← Other methods</button>
      </div>
    </div>
  );
}
