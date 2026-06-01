import { fmtTime } from '../utils/dateUtils';

export default function AuditLogTable({ logs }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Time</th><th>Event</th><th>Sev</th><th>User</th>
            <th>IP</th><th>Risk</th><th>Details</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((l) => (
            <tr key={l.id}>
              <td className="mono">{fmtTime(l.createdAt)}</td>
              <td className="mono">{l.eventType}</td>
              <td><span className={`badge ${l.severity}`}>{l.severity}</span></td>
              <td>{l.username || '—'}</td>
              <td className="mono">{l.ipAddress || '—'}</td>
              <td>{l.riskScore ?? '—'}</td>
              <td className="muted">{l.details}</td>
            </tr>
          ))}
          {logs.length === 0 && <tr><td colSpan="7" className="muted">No events.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
