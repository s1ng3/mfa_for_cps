import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import RiskBadge from '../components/RiskBadge';
import { methodLabel, methodRoute } from '../utils/riskUtils';

export default function MfaSelectionPage() {
  const { pendingLogin, refreshMe } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);

  // On a hard refresh the pendingLogin is gone but the PENDING_MFA token survives — recover state.
  useEffect(() => {
    if (!pendingLogin) refreshMe().then(setProfile).catch(() => {});
  }, [pendingLogin, refreshMe]);

  const required = pendingLogin?.requiredMethod || profile?.requiredMfaMethod;
  const score = pendingLogin?.riskScore ?? profile?.riskScore ?? 0;
  const level = pendingLogin?.riskLevel || levelFromScore(score);
  const reasons = pendingLogin?.riskReasons;

  const alternatives = ['EMAIL_OTP', 'SMS_OTP', 'WEBAUTHN'];

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Multi-Factor Authentication</h1>
        <div className="auth-sub">
          The risk engine evaluated this login and selected a required second factor.
        </div>

        <div className="card">
          <RiskBadge level={level} score={score} reasons={reasons} />
        </div>

        <div className="alert-box info mt">
          Required method: <strong>{methodLabel[required] || required}</strong>
        </div>

        <button className="btn" onClick={() => navigate(methodRoute[required] || '/mfa/email')}>
          Continue with {methodLabel[required] || required}
        </button>

        <div className="divider" />
        <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>Other available factors</div>
        <div className="grid cols-3">
          {alternatives.filter((m) => m !== required).map((m) => (
            <button key={m} className="btn inline secondary" onClick={() => navigate(methodRoute[m])}>
              {methodLabel[m]}
            </button>
          ))}
        </div>
        <button className="btn secondary mt" onClick={() => navigate('/mfa/recovery')}>
          Use a backup recovery code
        </button>
      </div>
    </div>
  );
}

function levelFromScore(s) {
  if (s <= 30) return 'LOW';
  if (s <= 60) return 'MEDIUM';
  if (s <= 80) return 'HIGH';
  return 'CRITICAL';
}
