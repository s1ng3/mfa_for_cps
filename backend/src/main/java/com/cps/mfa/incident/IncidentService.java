package com.cps.mfa.incident;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.IncidentStatus;
import com.cps.mfa.common.Severity;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/** Creates and queries incidents. Incident codes follow {@code INC-YYYY-NNNN}. */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository repository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public synchronized Incident create(Severity severity, String title, String description, User user) {
        Incident incident = new Incident();
        incident.setIncidentCode(nextCode());
        incident.setSeverity(severity);
        incident.setStatus(IncidentStatus.NEW);
        incident.setTitle(title);
        incident.setDescription(description);
        if (user != null) {
            incident.setUserId(user.getId());
            incident.setUsername(user.getUsername());
        }
        Incident saved = repository.save(incident);

        auditService.log(AuditEventType.INCIDENT_CREATED, severity, user, null, null,
                saved.getIncidentCode() + ": " + title);
        notificationService.notify("SECURITY_INCIDENT_CREATED", severity, user,
                saved.getIncidentCode() + " - " + title);
        return saved;
    }

    public List<Incident> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Incident findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> com.cps.mfa.common.ApiException.notFound("Incident not found: " + id));
    }

    public Incident save(Incident incident) {
        return repository.save(incident);
    }

    /** Generates the next sequential incident code for the current year. */
    private String nextCode() {
        String prefix = "INC-" + Year.now().getValue() + "-";
        long count = repository.countByIncidentCodeStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }
}
