import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { landingPath } from '../utils/nav';
import { mfaApi } from '../services/mfaApi';
import { apiError } from '../api/client';

export default function RecoveryCodePage() {
  const { completeMfa } = useAuth();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const verify = async () => {
    setBusy(true); setError(null);
    try {
      const me = await mfaApi.verifyRecovery(code.trim());
      completeMfa(me);
      navigate(landingPath(me), { replace: true });
    } catch (e) { setError(apiError(e)); }
    finally { setBusy(false); }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Backup Recovery Code</h1>
        <div className="auth-sub">
          Enter one of your single-use backup codes. Each code works only once and its use is audited.
        </div>
        <label>Recovery code</label>
        <input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())}
               placeholder="XXXX-XXXX" className="mono" />
        <button className="btn" disabled={busy || !code} onClick={verify}>
          {busy ? 'Verifying…' : 'Verify recovery code'}
        </button>
        {error && <div className="alert-box error">{error}</div>}
        <button className="btn secondary mt" onClick={() => navigate('/mfa')}>← Other methods</button>
      </div>
    </div>
  );
}
