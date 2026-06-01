import { useState } from 'react';
import { resetDeviceFingerprint } from '../utils/deviceFingerprint';

// Credentials + demo "risk simulation" toggles. The toggles map to backend LoginRequest flags
// so a presenter can deliberately push the risk score into HIGH/CRITICAL bands.
export default function LoginForm({ onSubmit, busy }) {
  const [form, setForm] = useState({
    username: '', password: '',
    simulateNewDevice: false, simulateUnknownIp: false, simulateOutsideHours: false,
  });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const submit = (e) => {
    e.preventDefault();
    if (form.simulateNewDevice) resetDeviceFingerprint(); // present an unrecognised fingerprint
    onSubmit(form);
  };

  return (
    <form onSubmit={submit}>
      <label>Username</label>
      <input autoFocus value={form.username} onChange={(e) => set('username', e.target.value)}
             placeholder="operator1" />

      <label>Password</label>
      <input type="password" value={form.password} onChange={(e) => set('password', e.target.value)}
             placeholder="••••••••" />

      <div className="divider" />
      <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>Demo risk simulation</div>
      <div className="checkbox-row">
        <input id="nd" type="checkbox" checked={form.simulateNewDevice}
               onChange={(e) => set('simulateNewDevice', e.target.checked)} />
        <label htmlFor="nd">New / unrecognised device (+25)</label>
      </div>
      <div className="checkbox-row">
        <input id="ip" type="checkbox" checked={form.simulateUnknownIp}
               onChange={(e) => set('simulateUnknownIp', e.target.checked)} />
        <label htmlFor="ip">Unknown IP address (+25)</label>
      </div>
      <div className="checkbox-row">
        <input id="oh" type="checkbox" checked={form.simulateOutsideHours}
               onChange={(e) => set('simulateOutsideHours', e.target.checked)} />
        <label htmlFor="oh">Outside working hours (+15)</label>
      </div>

      <button className="btn" disabled={busy || !form.username || !form.password}>
        {busy ? 'Authenticating…' : 'Sign in'}
      </button>
    </form>
  );
}
