import { useEffect, useState } from 'react';
import { incidentApi } from '../services/incidentApi';
import { apiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import IncidentTable from '../components/IncidentTable';
import { fmtTime } from '../utils/dateUtils';

const STATUSES = ['NEW', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE'];

export default function IncidentsPage() {
  const { profile, hasPermission } = useAuth();
  const [incidents, setIncidents] = useState([]);
  const [selected, setSelected] = useState(null);
  const [note, setNote] = useState('');
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  const canManage = hasPermission('INCIDENT_MANAGE');

  const load = () => incidentApi.list().then((list) => {
    setIncidents(list);
    if (selected) setSelected(list.find((i) => i.id === selected.id) || null);
  }).catch((e) => setError(apiError(e)));

  useEffect(() => { load(); const id = setInterval(load, 10000); return () => clearInterval(id); }, []); // eslint-disable-line

  const act = async (fn, msg) => {
    setError(null); setToast(null);
    try { await fn(); setToast(msg); setNote(''); load(); } catch (e) { setError(apiError(e)); }
  };

  return (
    <div>
      <h2 className="mb">Incident Response</h2>
      {toast && <div className="alert-box ok">{toast}</div>}
      {error && <div className="alert-box error">{error}</div>}

      <div className="grid cols-2">
        <div>
          <IncidentTable incidents={incidents} onSelect={setSelected} selectedId={selected?.id} />
        </div>

        <div className="card">
          {!selected ? <div className="muted">Select an incident to investigate.</div> : (
            <>
              <div className="spread">
                <h3 style={{ margin: 0 }}>{selected.incidentCode}</h3>
                <span className={`badge ${selected.severity}`}>{selected.severity}</span>
              </div>
              <div style={{ fontSize: 16, fontWeight: 600, margin: '8px 0' }}>{selected.title}</div>
              <div className="muted" style={{ fontSize: 13 }}>{selected.description}</div>

              <div className="divider" />
              <div className="row"><span className="muted">Status</span><span className="pill">{selected.status}</span></div>
              <div className="row mt"><span className="muted">User</span><span>{selected.username || '—'}</span></div>
              <div className="row mt"><span className="muted">Assigned</span><span>{selected.assignedTo || '—'}</span></div>
              <div className="row mt"><span className="muted">Created</span><span className="mono">{fmtTime(selected.createdAt)}</span></div>
              {selected.resolvedAt && <div className="row mt"><span className="muted">Resolved</span><span className="mono">{fmtTime(selected.resolvedAt)}</span></div>}

              {selected.investigationNotes && (
                <div className="alert-box info mt" style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>
                  {selected.investigationNotes}
                </div>
              )}

              {canManage && (
                <>
                  <div className="divider" />
                  <label>Investigation note</label>
                  <textarea rows={2} value={note} onChange={(e) => setNote(e.target.value)}
                            style={{ width: '100%', background: 'var(--bg)', color: 'var(--text)', border: '1px solid var(--border)', borderRadius: 8, padding: 10 }} />
                  <div className="row mt">
                    <button className="btn inline secondary"
                            onClick={() => act(() => incidentApi.assign(selected.id, profile.username), 'Incident assigned to you')}>
                      Assign to me
                    </button>
                  </div>
                  <label>Change status</label>
                  <div className="row">
                    {STATUSES.map((s) => (
                      <button key={s} className="btn inline secondary"
                              onClick={() => act(() => incidentApi.setStatus(selected.id, s, note), `Status → ${s}`)}>
                        {s}
                      </button>
                    ))}
                  </div>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
