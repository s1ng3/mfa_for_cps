import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { landingPath } from '../utils/nav';
import { mfaApi } from '../services/mfaApi';
import { apiError } from '../api/client';
import OtpInput from '../components/OtpInput';

export default function EmailOtpPage() {
  const { completeMfa } = useAuth();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  const send = async () => {
    setError(null);
    try { const r = await mfaApi.sendEmail(); setSent(true); setInfo(r.message); }
    catch (e) { setError(apiError(e)); }
  };

  useEffect(() => { send(); /* auto-send on mount */ // eslint-disable-next-line
  }, []);

  const verify = async () => {
    setBusy(true); setError(null);
    try {
      const me = await mfaApi.verifyEmail(code);
      completeMfa(me);
      navigate(landingPath(me), { replace: true });
    } catch (e) { setError(apiError(e)); setCode(''); }
    finally { setBusy(false); }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Email OTP</h1>
        <div className="auth-sub">
          A 6-digit code was sent to your email. In this demo it is printed to the
          <strong> backend console</strong> (mocked email sink).
        </div>
        {info && <div className="alert-box ok">{info}</div>}
        <label>Enter the 6-digit code</label>
        <OtpInput value={code} onChange={setCode} />
        <button className="btn" disabled={busy || code.length < 6} onClick={verify}>
          {busy ? 'Verifying…' : 'Verify'}
        </button>
        <button className="btn secondary" onClick={send} disabled={busy}>Resend code</button>
        {error && <div className="alert-box error">{error}</div>}
        <button className="btn secondary mt" onClick={() => navigate('/mfa')}>← Other methods</button>
      </div>
    </div>
  );
}
