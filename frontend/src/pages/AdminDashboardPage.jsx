import { useEffect, useState } from 'react';
import { adminApi } from '../services/adminApi';
import { apiError } from '../api/client';
import SecurityChart from '../components/SecurityChart';
import IncidentTable from '../components/IncidentTable';
import { fmtTime } from '../utils/dateUtils';

export default function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const load = () => adminApi.dashboard().then(setData).catch((e) => setError(apiError(e)));
    load();
    const id = setInterval(load, 10000);
    return () => clearInterval(id);
  }, []);

  if (error) return <div className="alert-box error">{error}</div>;
  if (!data) return <div className="muted">Loading dashboard…</div>;

  return (
    <div>
      <h2 className="mb">Security Operations Dashboard</h2>

      <div className="grid cols-4 mb">
        <Stat label="Total Users" value={data.totalUsers} />
        <Stat label="Active Sessions" value={data.activeSessions} />
        <Stat label="Open Incidents" value={data.openIncidents} accent="var(--high)" />
        <Stat label="Unread Alerts" value={data.unreadAlerts} accent="var(--critical)" />
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h3>Recent Events (by type)</h3>
          <SecurityChart events={data.recentEvents} />
        </div>
        <div className="card">
          <h3>Security Alerts</h3>
          <div style={{ maxHeight: 240, overflowY: 'auto' }}>
            {data.recentAlerts.length === 0 && <div className="muted">No alerts.</div>}
            {data.recentAlerts.map((a) => (
              <div key={a.id} className="spread" style={{ padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                <div>
                  <span className={`badge ${a.severity}`}>{a.alertType}</span>
                  <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>{a.message}</div>
                </div>
                <span className="mono muted" style={{ fontSize: 11 }}>{fmtTime(a.createdAt)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="section-title">Recent Incidents</div>
      <IncidentTable incidents={data.recentIncidents} />
    </div>
  );
}

function Stat({ label, value, accent }) {
  return (
    <div className="card">
      <div className="stat" style={{ color: accent || 'var(--text)' }}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}
