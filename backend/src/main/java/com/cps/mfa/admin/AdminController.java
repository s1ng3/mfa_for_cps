package com.cps.mfa.admin;

import com.cps.mfa.audit.AuditDto;
import com.cps.mfa.audit.AuditLogRepository;
import com.cps.mfa.common.IncidentStatus;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.common.SimpleResponse;
import com.cps.mfa.incident.Incident;
import com.cps.mfa.incident.IncidentService;
import com.cps.mfa.notification.AlertService;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.rbac.Permissions;
import com.cps.mfa.session.AuthContext;
import com.cps.mfa.session.SessionDto;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.session.UserSessionRepository;
import com.cps.mfa.user.CreateUserRequest;
import com.cps.mfa.user.UserDto;
import com.cps.mfa.user.UserRepository;
import com.cps.mfa.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administrative dashboard: posture overview, user management and session control. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final SessionService sessionService;
    private final IncidentService incidentService;
    private final AlertService alertService;
    private final AuditLogRepository auditLogRepository;
    private final AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    public DashboardDto dashboard(HttpServletRequest http) {
        // Visible to anyone who can view sessions, audit or incidents (admin + security officer).
        requireAny(http, "VIEW_DASHBOARD");

        long activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE).size();
        long openIncidents = incidentService.findAll().stream()
                .filter(i -> i.getStatus() == IncidentStatus.NEW || i.getStatus() == IncidentStatus.INVESTIGATING)
                .count();
        List<Incident> recentIncidents = incidentService.findAll().stream().limit(10).toList();
        List<AuditDto> recentEvents = auditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20))
                .map(AuditDto::from).getContent();

        return new DashboardDto(userRepository.count(), activeSessions, openIncidents,
                alertService.unreadCount(), alertService.recent(), recentIncidents, recentEvents);
    }

    // ---- Users ----------------------------------------------------------------------------

    @GetMapping("/users")
    public List<UserDto> users(HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_MANAGE_USERS,
                RequestMeta.from(http), "LIST_USERS");
        return userService.list();
    }

    @PostMapping("/users")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest req, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_MANAGE_USERS,
                RequestMeta.from(http), "CREATE_USER");
        return userService.create(req);
    }

    @PutMapping("/users/{id}/lock")
    public UserDto lock(@PathVariable Long id, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_MANAGE_USERS,
                RequestMeta.from(http), "LOCK_USER");
        return userService.lock(id);
    }

    @PutMapping("/users/{id}/unlock")
    public UserDto unlock(@PathVariable Long id, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_MANAGE_USERS,
                RequestMeta.from(http), "UNLOCK_USER");
        return userService.unlock(id);
    }

    // ---- Sessions -------------------------------------------------------------------------

    @GetMapping("/sessions")
    public List<SessionDto> sessions(HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_VIEW_SESSIONS,
                RequestMeta.from(http), "VIEW_SESSIONS");
        return sessionRepository.findAllByOrderByCreatedAtDesc().stream().map(SessionDto::from).toList();
    }

    @PostMapping("/sessions/{id}/terminate")
    public SimpleResponse terminate(@PathVariable Long id, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.ADMIN_TERMINATE_SESSIONS,
                RequestMeta.from(http), "TERMINATE_SESSION");
        UserSession session = sessionService.findById(id);
        sessionService.terminate(session, "Terminated by administrator");
        return SimpleResponse.ok("Session terminated");
    }

    /** The dashboard is useful to both admins and security officers — accept any of their view perms. */
    private void requireAny(HttpServletRequest http, String action) {
        var user = AuthContext.currentUser();
        if (authorizationService.hasPermission(user, Permissions.ADMIN_VIEW_SESSIONS)
                || authorizationService.hasPermission(user, Permissions.AUDIT_VIEW)
                || authorizationService.hasPermission(user, Permissions.INCIDENT_VIEW)) {
            return;
        }
        // Falls through to the standard denied-path (audit + alert + 403).
        authorizationService.require(user, Permissions.ADMIN_VIEW_SESSIONS, RequestMeta.from(http), action);
    }
}
