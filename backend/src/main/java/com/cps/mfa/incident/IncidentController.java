package com.cps.mfa.incident;

import com.cps.mfa.common.IncidentStatus;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.rbac.Permissions;
import com.cps.mfa.session.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Incident queue + workflow for ADMIN and SECURITY_OFFICER. */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentWorkflowService workflowService;
    private final AuthorizationService authorizationService;

    public record CreateIncidentRequest(@NotNull Severity severity, @NotBlank String title,
                                        String description) {
    }

    public record AssignRequest(@NotBlank String assignedTo) {
    }

    public record StatusRequest(@NotNull IncidentStatus status, String note) {
    }

    @GetMapping
    public List<Incident> list(HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.INCIDENT_VIEW,
                RequestMeta.from(http), "VIEW_INCIDENTS");
        return incidentService.findAll();
    }

    @GetMapping("/{id}")
    public Incident get(@PathVariable Long id, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.INCIDENT_VIEW,
                RequestMeta.from(http), "VIEW_INCIDENT");
        return incidentService.findById(id);
    }

    @PostMapping
    public Incident create(@RequestBody CreateIncidentRequest req, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.INCIDENT_MANAGE,
                RequestMeta.from(http), "CREATE_INCIDENT");
        return incidentService.create(req.severity(), req.title(), req.description(), null);
    }

    @PutMapping("/{id}/assign")
    public Incident assign(@PathVariable Long id, @RequestBody AssignRequest req, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.INCIDENT_MANAGE,
                RequestMeta.from(http), "ASSIGN_INCIDENT");
        return workflowService.assign(id, req.assignedTo());
    }

    @PutMapping("/{id}/status")
    public Incident status(@PathVariable Long id, @RequestBody StatusRequest req, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.INCIDENT_MANAGE,
                RequestMeta.from(http), "UPDATE_INCIDENT_STATUS");
        return workflowService.changeStatus(id, req.status(), req.note());
    }
}
