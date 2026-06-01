package com.cps.mfa.session;

import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.common.Severity;
import com.cps.mfa.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Flags session anomalies — currently concurrent active sessions for the same user originating
 * from different IP addresses, a classic indicator of credential sharing or hijack. Alerts are
 * de-duplicated per user so the monitor does not spam the SOC every tick.
 */
@Service
@RequiredArgsConstructor
public class SessionAnomalyDetector {

    private final UserSessionRepository sessionRepository;
    private final NotificationService notificationService;

    private final Set<String> alreadyAlerted = new HashSet<>();

    public void scan() {
        List<UserSession> active = sessionRepository.findByStatus(SessionStatus.ACTIVE);

        active.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getUsername()))
                .forEach((username, sessions) -> {
                    long distinctIps = sessions.stream()
                            .map(UserSession::getIpAddress)
                            .filter(ip -> ip != null)
                            .distinct().count();
                    if (sessions.size() > 1 && distinctIps > 1) {
                        if (alreadyAlerted.add(username)) {
                            notificationService.notify("SESSION_ANOMALY", Severity.HIGH,
                                    sessions.get(0).getUser(),
                                    "Concurrent active sessions for " + username
                                            + " from " + distinctIps + " distinct IP addresses.");
                        }
                    } else {
                        alreadyAlerted.remove(username);
                    }
                });
    }
}
