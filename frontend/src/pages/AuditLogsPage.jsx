import { useEffect, useState } from 'react';
import { adminApi } from '../services/adminApi';
import { siemApi } from '../services/siemApi';
import { apiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import AuditLogTable from '../components/AuditLogTable';

export default function AuditLogsPage() {
  const { hasPermission } = useAuth();
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState(null);

  const load = () => adminApi.auditLogs(0, 200).then(setLogs).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); }, []);

  const filtered = logs.filter((l) =>
    !filter ||
    l.eventType.toLowerCase().includes(filter.toLowerCase()) ||
    (l.username || '').toLowerCase().includes(filter.toLowerCase()) ||
    (l.details || '').toLowerCase().includes(filter.toLowerCase()));

  const canExport = hasPermission('SIEM_EXPORT');

  return (
    <div>
      <div className="spread mb">
        <h2 style={{ margin: 0 }}>Audit Trail</h2>
        <div className="row">
          {canExport && <button className="btn inline secondary" onClick={() => siemApi.exportJson()}>⬇ SIEM JSON</button>}
          {canExport && <button className="btn inline secondary" onClick={() => siemApi.exportCsv()}>⬇ SIEM CSV</button>}
          <button className="btn inline secondary" onClick={load}>Refresh</button>
        </div>
      </div>

      {error && <div className="alert-box error">{error}</div>}
      {!canExport && <div className="alert-box info">SIEM export is restricted to the SECURITY_OFFICER role.</div>}

      <input className="mb" placeholder="Filter by event, user or details…"
             value={filter} onChange={(e) => setFilter(e.target.value)} />
      <AuditLogTable logs={filtered} />
    </div>
  );
}
