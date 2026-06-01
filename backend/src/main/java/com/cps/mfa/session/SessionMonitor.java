package com.cps.mfa.session;

import com.cps.mfa.common.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/** Background task that expires timed-out sessions and runs anomaly detection. */
@Service
@RequiredArgsConstructor
public class SessionMonitor {

    private final UserSessionRepository sessionRepository;
    private final SessionService sessionService;
    private final SessionAnomalyDetector anomalyDetector;

    /** Runs every 30 seconds: idle/absolute timeouts are enforced even without client traffic. */
    @Scheduled(fixedRate = 30_000)
    public void monitor() {
        List<UserSession> active = sessionRepository.findByStatus(SessionStatus.ACTIVE);
        for (UserSession session : active) {
            if (sessionService.isTimedOut(session)) {
                sessionService.expire(session, "Session timed out (monitor: idle/absolute)");
            }
        }
        // Also clean up PENDING_MFA sessions abandoned past their absolute window.
        for (UserSession session : sessionRepository.findByStatus(SessionStatus.PENDING_MFA)) {
            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(java.time.Instant.now())) {
                sessionService.expire(session, "Abandoned pre-MFA session expired");
            }
        }
        anomalyDetector.scan();
    }
}
