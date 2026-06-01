package com.cps.mfa.incident;

import com.cps.mfa.common.IncidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

/** Drives an incident through its workflow: assignment, status changes and investigation notes. */
@Service
@RequiredArgsConstructor
public class IncidentWorkflowService {

    private final IncidentService incidentService;

    public Incident assign(Long id, String assignee) {
        Incident incident = incidentService.findById(id);
        incident.setAssignedTo(assignee);
        if (incident.getStatus() == IncidentStatus.NEW) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
        }
        return incidentService.save(incident);
    }

    public Incident changeStatus(Long id, IncidentStatus status, String note) {
        Incident incident = incidentService.findById(id);
        incident.setStatus(status);
        if (StringUtils.hasText(note)) {
            appendNote(incident, note);
        }
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.FALSE_POSITIVE) {
            incident.setResolvedAt(Instant.now());
        }
        return incidentService.save(incident);
    }

    public Incident addNote(Long id, String note) {
        Incident incident = incidentService.findById(id);
        appendNote(incident, note);
        return incidentService.save(incident);
    }

    private void appendNote(Incident incident, String note) {
        String existing = incident.getInvestigationNotes();
        String stamped = "[" + Instant.now() + "] " + note;
        incident.setInvestigationNotes(existing == null ? stamped : existing + "\n" + stamped);
    }
}
