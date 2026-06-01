import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SessionTimer from './SessionTimer';

const NAV = [
  { to: '/hmi', label: 'HMI Dashboard', perm: 'HMI_VIEW' },
  { to: '/admin', label: 'Security Dashboard', anyPerm: ['ADMIN_VIEW_SESSIONS', 'AUDIT_VIEW', 'INCIDENT_VIEW'] },
  { to: '/admin/users', label: 'Users', perm: 'ADMIN_MANAGE_USERS' },
  { to: '/admin/sessions', label: 'Sessions', perm: 'ADMIN_VIEW_SESSIONS' },
  { to: '/incidents', label: 'Incidents', perm: 'INCIDENT_VIEW' },
  { to: '/audit', label: 'Audit & SIEM', perm: 'AUDIT_VIEW' },
];

export default function Layout() {
  const { profile, logout, hasPermission } = useAuth();
  const navigate = useNavigate();

  const visible = NAV.filter((n) => {
    if (n.perm) return hasPermission(n.perm);
    if (n.anyPerm) return n.anyPerm.some((p) => hasPermission(p));
    return true;
  });

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          🔐 CPS MFA Gateway
          <br />
          <small>Adaptive Security · HMI/SCADA</small>
        </div>
        {visible.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.to === '/admin'}
                   className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
            {n.label}
          </NavLink>
        ))}
        <div style={{ flex: 1 }} />
        <div className="nav-link" onClick={onLogout}>↩ Logout</div>
      </aside>

      <main className="main">
        <div className="topbar">
          <div className="user">
            <span>Signed in as <strong style={{ color: 'var(--text)' }}>{profile?.username}</strong></span>
            {profile?.roles?.map((r) => <span key={r} className="pill">{r}</span>)}
          </div>
          <div className="user">
            <SessionTimer />
            <button className="btn inline secondary" onClick={onLogout}>Logout</button>
          </div>
        </div>
        <Outlet />
      </main>
    </div>
  );
}
