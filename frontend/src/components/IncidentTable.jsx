import { fmtTime } from '../utils/dateUtils';

export default function IncidentTable({ incidents, onSelect, selectedId }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr><th>Code</th><th>Title</th><th>Severity</th><th>Status</th><th>User</th><th>Assigned</th><th>Created</th></tr>
        </thead>
        <tbody>
          {incidents.map((i) => (
            <tr key={i.id} onClick={() => onSelect?.(i)}
                style={{ cursor: onSelect ? 'pointer' : 'default', background: selectedId === i.id ? 'var(--panel-2)' : undefined }}>
              <td className="mono">{i.incidentCode}</td>
              <td>{i.title}</td>
              <td><span className={`badge ${i.severity}`}>{i.severity}</span></td>
              <td><span className="pill">{i.status}</span></td>
              <td>{i.username || '—'}</td>
              <td>{i.assignedTo || '—'}</td>
              <td className="mono">{fmtTime(i.createdAt)}</td>
            </tr>
          ))}
          {incidents.length === 0 && <tr><td colSpan="7" className="muted">No incidents.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
