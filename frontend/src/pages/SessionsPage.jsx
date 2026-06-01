import { useEffect, useState } from 'react';
import { adminApi } from '../services/adminApi';
import { apiError } from '../api/client';
import { fmtTime } from '../utils/dateUtils';

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  const load = () => adminApi.sessions().then(setSessions).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); const id = setInterval(load, 8000); return () => clearInterval(id); }, []);

  const terminate = async (id) => {
    setError(null); setToast(null);
    try { await adminApi.terminateSession(id); setToast(`Session ${id} terminated`); load(); }
    catch (e) { setError(apiError(e)); }
  };

  return (
    <div>
      <h2 className="mb">Active & Historical Sessions</h2>
      {toast && <div className="alert-box ok">{toast}</div>}
      {error && <div className="alert-box error">{error}</div>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>User</th><th>Status</th><th>IP</th><th>Risk</th><th>Step-up</th><th>Created</th><th>Last activity</th><th>Expires</th><th></th></tr>
          </thead>
          <tbody>
            {sessions.map((s) => (
              <tr key={s.id}>
                <td>{s.username}</td>
                <td><span className={'badge ' + statusBadge(s.status)}>{s.status}</span></td>
                <td className="mono">{s.ipAddress}</td>
                <td>{s.riskScore}</td>
                <td>{s.stepUpValid ? <span className="badge LOW">valid</span> : '—'}</td>
                <td className="mono">{fmtTime(s.createdAt)}</td>
                <td className="mono">{fmtTime(s.lastActivityAt)}</td>
                <td className="mono">{fmtTime(s.expiresAt)}</td>
                <td>
                  {s.status === 'ACTIVE' || s.status === 'PENDING_MFA'
                    ? <button className="btn inline danger" onClick={() => terminate(s.id)}>Terminate</button>
                    : null}
                </td>
              </tr>
            ))}
            {sessions.length === 0 && <tr><td colSpan="9" className="muted">No sessions.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function statusBadge(status) {
  return { ACTIVE: 'LOW', PENDING_MFA: 'MEDIUM', EXPIRED: 'HIGH', TERMINATED: 'CRITICAL', BLOCKED: 'CRITICAL' }[status] || 'MEDIUM';
}
