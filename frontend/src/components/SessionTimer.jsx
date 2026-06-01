import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/authApi';
import { fmtClock } from '../utils/dateUtils';

const IDLE_LIMIT_SEC = 5 * 60; // mirrors backend app.session.idle-timeout-minutes

// Shows the live idle/absolute countdown for the current session. Polling /session/current is a
// passive read (it does NOT reset the idle timer server-side), so the displayed countdown is honest.
export default function SessionTimer() {
  const { logout, clear } = useAuth();
  const [session, setSession] = useState(null);
  const [now, setNow] = useState(Date.now());
  const tick = useRef();

  useEffect(() => {
    let alive = true;
    const poll = async () => {
      try {
        const s = await authApi.currentSession();
        if (!alive) return;
        if (s.status !== 'ACTIVE') { clear('Your session has ended.'); return; }
        setSession(s);
      } catch {
        // 401 is handled by the global interceptor (auto-logout).
      }
    };
    poll();
    const id = setInterval(poll, 20000);
    tick.current = setInterval(() => setNow(Date.now()), 1000);
    return () => { alive = false; clearInterval(id); clearInterval(tick.current); };
  }, [clear]);

  if (!session) return null;

  const idleRemaining = IDLE_LIMIT_SEC - (now - new Date(session.lastActivityAt).getTime()) / 1000;
  const absRemaining = (new Date(session.expiresAt).getTime() - now) / 1000;
  const remaining = Math.min(idleRemaining, absRemaining);

  if (remaining <= 0) {
    // Force a re-check; the next protected call will 401 and log us out.
    logout();
  }

  const color = remaining < 60 ? 'var(--critical)' : remaining < 120 ? 'var(--medium)' : 'var(--muted)';
  return (
    <span className="timer" style={{ color }} title="Session expires on idle / absolute timeout">
      ⏱ {fmtClock(remaining)}
    </span>
  );
}
