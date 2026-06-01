import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { landingPath } from './utils/nav';
import Layout from './components/Layout';

import LoginPage from './pages/LoginPage';
import MfaSelectionPage from './pages/MfaSelectionPage';
import EmailOtpPage from './pages/EmailOtpPage';
import SmsOtpPage from './pages/SmsOtpPage';
import WebAuthnPage from './pages/WebAuthnPage';
import RecoveryCodePage from './pages/RecoveryCodePage';
import HmiDashboardPage from './pages/HmiDashboardPage';
import StepUpMfaPage from './pages/StepUpMfaPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AuditLogsPage from './pages/AuditLogsPage';
import IncidentsPage from './pages/IncidentsPage';
import SessionsPage from './pages/SessionsPage';
import UsersPage from './pages/UsersPage';

function Splash() {
  return <div className="auth-wrap"><div className="muted">Loading gateway…</div></div>;
}

// Requires a fully authenticated (ACTIVE) session.
function RequireAuth({ children }) {
  const { isAuthenticated, isMfaPending, loading } = useAuth();
  if (loading) return <Splash />;
  if (!isAuthenticated) return <Navigate to={isMfaPending ? '/mfa' : '/login'} replace />;
  return children;
}

// Requires a half-authenticated (PENDING_MFA) session — the MFA stage.
function RequireMfaPending({ children }) {
  const { isAuthenticated, isMfaPending, loading } = useAuth();
  const profile = useAuth().profile;
  if (loading) return <Splash />;
  if (isAuthenticated) return <Navigate to={landingPath(profile)} replace />;
  if (!isMfaPending) return <Navigate to="/login" replace />;
  return children;
}

function PublicOnly({ children }) {
  const { isAuthenticated, isMfaPending, profile, loading } = useAuth();
  if (loading) return <Splash />;
  if (isAuthenticated) return <Navigate to={landingPath(profile)} replace />;
  if (isMfaPending) return <Navigate to="/mfa" replace />;
  return children;
}

export default function App() {
  useLocation(); // re-render on navigation
  return (
    <Routes>
      <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />

      {/* MFA stage */}
      <Route path="/mfa" element={<RequireMfaPending><MfaSelectionPage /></RequireMfaPending>} />
      <Route path="/mfa/email" element={<RequireMfaPending><EmailOtpPage /></RequireMfaPending>} />
      <Route path="/mfa/sms" element={<RequireMfaPending><SmsOtpPage /></RequireMfaPending>} />
      <Route path="/mfa/webauthn" element={<RequireMfaPending><WebAuthnPage /></RequireMfaPending>} />
      <Route path="/mfa/recovery" element={<RequireMfaPending><RecoveryCodePage /></RequireMfaPending>} />

      {/* Authenticated app */}
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/hmi" element={<HmiDashboardPage />} />
        <Route path="/step-up" element={<StepUpMfaPage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/users" element={<UsersPage />} />
        <Route path="/admin/sessions" element={<SessionsPage />} />
        <Route path="/audit" element={<AuditLogsPage />} />
        <Route path="/incidents" element={<IncidentsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
