import { useEffect, useState } from 'react';
import { adminApi } from '../services/adminApi';
import { apiError } from '../api/client';
import { fmtTime } from '../utils/dateUtils';

const ALL_ROLES = ['VIEWER', 'OPERATOR', 'ENGINEER', 'ADMIN', 'SECURITY_OFFICER'];

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);
  const [form, setForm] = useState({ username: '', password: '', email: '', phoneNumber: '', role: 'OPERATOR' });

  const load = () => adminApi.users().then(setUsers).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); }, []);

  const act = async (fn, msg) => {
    setError(null); setToast(null);
    try { await fn(); setToast(msg); load(); } catch (e) { setError(apiError(e)); }
  };

  const create = (e) => {
    e.preventDefault();
    act(() => adminApi.createUser({
      username: form.username, password: form.password, email: form.email,
      phoneNumber: form.phoneNumber, roles: [form.role],
    }), `User ${form.username} created`);
    setForm({ username: '', password: '', email: '', phoneNumber: '', role: 'OPERATOR' });
  };

  return (
    <div>
      <h2 className="mb">User Management</h2>
      {toast && <div className="alert-box ok">{toast}</div>}
      {error && <div className="alert-box error">{error}</div>}

      <div className="grid cols-2 mb">
        <div className="card">
          <h3>Create User</h3>
          <form onSubmit={create}>
            <label>Username</label>
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            <label>Password</label>
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            <label>Email</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <label>Phone</label>
            <input value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
            <label>Role</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {ALL_ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <button className="btn">Create user</button>
          </form>
        </div>
        <div className="card">
          <h3>Note</h3>
          <p className="muted" style={{ fontSize: 13 }}>
            Locking a user immediately terminates their active sessions and records
            an <span className="mono">ACCOUNT_LOCKED</span> audit event. Unlocking resets the
            failed-login and failed-MFA counters.
          </p>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>User</th><th>Email</th><th>Roles</th><th>Status</th><th>Fails (login/MFA)</th><th>Last login</th><th></th></tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td className="muted">{u.email}</td>
                <td>{u.roles.map((r) => <span key={r} className="pill">{r}</span>)}</td>
                <td>{u.accountStatus === 'LOCKED'
                  ? <span className="badge CRITICAL">LOCKED</span>
                  : <span className="badge LOW">{u.accountStatus}</span>}</td>
                <td className="mono">{u.failedLoginAttempts}/{u.failedMfaAttempts}</td>
                <td className="mono">{fmtTime(u.lastLoginAt)}</td>
                <td>
                  {u.accountStatus === 'LOCKED'
                    ? <button className="btn inline success" onClick={() => act(() => adminApi.unlockUser(u.id), `${u.username} unlocked`)}>Unlock</button>
                    : <button className="btn inline danger" onClick={() => act(() => adminApi.lockUser(u.id), `${u.username} locked`)}>Lock</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
