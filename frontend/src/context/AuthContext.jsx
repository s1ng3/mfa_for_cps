import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import client, { tokenStore, setUnauthorizedHandler } from '../api/client';
import { authApi } from '../services/authApi';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [profile, setProfile] = useState(null);      // MeResponse (ACTIVE => authenticated)
  const [pendingLogin, setPendingLogin] = useState(null); // LoginResponse during MFA stage
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);        // surfaced auto-logout reason

  const clear = useCallback((reason) => {
    tokenStore.clear();
    setProfile(null);
    setPendingLogin(null);
    if (reason) setNotice(reason);
  }, []);

  // Wire the axios 401 handler once.
  useEffect(() => {
    setUnauthorizedHandler((code) => {
      const msg = code === 'SESSION_EXPIRED'
        ? 'Your session expired and you were logged out.'
        : code === 'MFA_REQUIRED'
          ? 'Multi-factor authentication is required.'
          : null;
      // Don't nuke the pending-MFA token just because a protected call 401'd mid-flow.
      if (code !== 'MFA_REQUIRED') clear(msg);
    });
  }, [clear]);

  // Restore session on first load.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token) { setLoading(false); return; }
    authApi.me()
      .then((me) => setProfile(me))
      .catch(() => clear())
      .finally(() => setLoading(false));
  }, [clear]);

  const login = useCallback(async (payload) => {
    setNotice(null);
    const res = await authApi.login(payload);
    if (res.blocked) {
      return res; // caller shows the block screen; no token issued
    }
    tokenStore.set(res.mfaToken);
    setPendingLogin(res);
    setProfile(null);
    return res;
  }, []);

  // Called by MFA pages with the MeResponse returned on successful verification.
  const completeMfa = useCallback((me) => {
    setProfile(me);
    setPendingLogin(null);
  }, []);

  const refreshMe = useCallback(async () => {
    const me = await authApi.me();
    setProfile(me);
    return me;
  }, []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    clear();
  }, [clear]);

  const isAuthenticated = !!profile && profile.sessionStatus === 'ACTIVE';
  const isMfaPending = !!tokenStore.get() && !isAuthenticated;

  const hasRole = (role) => profile?.roles?.includes(role);
  const hasPermission = (perm) => profile?.permissions?.includes(perm);

  const value = {
    profile, pendingLogin, loading, notice, setNotice,
    isAuthenticated, isMfaPending,
    login, completeMfa, refreshMe, logout, clear,
    hasRole, hasPermission,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
