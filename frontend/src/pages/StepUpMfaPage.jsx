import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { hmiApi, stepUpApi } from '../services/hmiApi';
import { apiError } from '../api/client';
import { performAuthentication } from '../utils/webauthn';

// Replays the original action once step-up succeeds (the session now carries a step-up window).
const REPLAY = {
  CHANGE_MOTOR_SPEED: (v) => hmiApi.changeMotorSpeed(v),
  CHANGE_TEMPERATURE_SETPOINT: (v) => hmiApi.changeTemperature(v),
  CHANGE_PRESSURE_SETPOINT: (v) => hmiApi.changePressure(v),
  RESET_EMERGENCY_STOP: () => hmiApi.resetEmergencyStop(),
};

export default function StepUpMfaPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  const actionType = state?.actionType;
  const value = state?.value;

  const [options, setOptions] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  useEffect(() => {
    if (!actionType) return;
    stepUpApi.request(actionType)
      .then((r) => { setOptions(r); setInfo('Step-up challenge issued. Confirm with your strong authenticator.'); })
      .catch((e) => setError(apiError(e)));
  }, [actionType]);

  if (!actionType) {
    return (
      <div className="card">
        <h3>Step-up MFA</h3>
        <div className="muted">No pending critical action. Return to the HMI dashboard.</div>
        <button className="btn secondary mt" onClick={() => navigate('/hmi')}>Back to HMI</button>
      </div>
    );
  }

  const verifyAndExecute = async () => {
    setBusy(true); setError(null);
    try {
      const assertion = await performAuthentication(options?.webauthn);
      await stepUpApi.verify(assertion.credentialId);
      // Step-up satisfied — replay the original critical action.
      const result = await REPLAY[actionType]?.(value);
      navigate('/hmi', { replace: true, state: undefined });
      // (HMI page will reflect the executed action on its next poll.)
      return result;
    } catch (e) {
      setError(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>🛡 Step-up MFA Required</h1>
        <div className="auth-sub">
          The action <strong>{actionType}</strong>{value != null ? <> → <span className="mono">{String(value)}</span></> : null} is
          a critical CPS operation. Confirm with a strong (WebAuthn/FIDO2) factor to proceed.
        </div>

        <div className="alert-box info">
          Audit trail: <span className="mono">CRITICAL_ACTION_REQUESTED → STEP_UP_MFA_REQUIRED → STEP_UP_MFA_SUCCESS → CPS_ACTION_EXECUTED</span>
        </div>

        <button className="btn" disabled={busy} onClick={verifyAndExecute}>
          {busy ? 'Verifying…' : '🔑 Confirm with strong authenticator & execute'}
        </button>
        {info && <div className="alert-box ok">{info}</div>}
        {error && <div className="alert-box error">{error}</div>}
        <button className="btn secondary mt" onClick={() => navigate('/hmi')}>Cancel</button>
      </div>
    </div>
  );
}
