import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../api/client';
import LoginForm from '../components/LoginForm';
import RiskBadge from '../components/RiskBadge';

export default function LoginPage() {
  const { login, notice } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [blocked, setBlocked] = useState(null);

  const onSubmit = async (payload) => {
    setBusy(true); setError(null); setBlocked(null);
    try {
      const res = await login(payload);
      if (res.blocked) { setBlocked(res); return; }
      navigate('/mfa', { replace: true });
    } catch (e) {
      setError(apiError(e, 'Login failed'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>CPS MFA Security Gateway</h1>
        <div className="auth-sub">
          Operator access to the HMI/SCADA workstation is brokered through adaptive,
          risk-based multi-factor authentication.
        </div>

        {notice && <div className="alert-box warn">{notice}</div>}

        {blocked ? (
          <div>
            <div className="alert-box error">{blocked.message}</div>
            <div className="card mt">
              <RiskBadge level={blocked.riskLevel} score={blocked.riskScore} reasons={blocked.riskReasons} />
            </div>
            <button className="btn secondary mt" onClick={() => setBlocked(null)}>Back to login</button>
          </div>
        ) : (
          <>
            <LoginForm onSubmit={onSubmit} busy={busy} />
            {error && <div className="alert-box error">{error}</div>}
          </>
        )}

        <div className="divider" />
        <div className="muted" style={{ fontSize: 12 }}>
          Demo users (password <span className="mono">Password123!</span>):
          viewer1 · operator1 · engineer1 · admin1 · security1
        </div>
      </div>
    </div>
  );
}
