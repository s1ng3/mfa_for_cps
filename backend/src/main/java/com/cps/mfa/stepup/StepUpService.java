package com.cps.mfa.stepup;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.*;
import com.cps.mfa.hmi.*;
import com.cps.mfa.incident.IncidentService;
import com.cps.mfa.mfa.WebAuthnService;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.session.AuthContext;
import com.cps.mfa.session.SessionService;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step-up MFA for critical CPS actions. We always demand a strong (WebAuthn/FIDO2) factor here,
 * regardless of the original login method — critical control of the process is the highest bar.
 * A successful step-up stamps a short validity window on the session.
 */
@Service
@RequiredArgsConstructor
public class StepUpService {

    private final WebAuthnService webAuthnService;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IncidentService incidentService;
    private final CpsActionRepository actionRepository;
    private final CpsActionService cpsActionService;

    /** Begins a step-up challenge (WebAuthn) for the named action and returns ceremony options. */
    public Map<String, Object> request(String actionType, RequestMeta meta) {
        User user = AuthContext.currentUser();
        UserSession session = AuthContext.currentSession();

        HmiActionType action = parse(actionType);
        auditService.log(AuditEventType.STEP_UP_MFA_REQUIRED, Severity.MEDIUM, user, meta,
                session.getRiskScore(), "Step-up challenge issued for " + action.name());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("method", MfaMethodType.WEBAUTHN);
        response.put("action", action.name());
        response.put("webauthn", webAuthnService.startAuthentication(user));
        return response;
    }

    /** Verifies the step-up assertion. On success the session may perform critical actions briefly. */
    @Transactional(noRollbackFor = ApiException.class)
    public boolean verify(String credentialId, RequestMeta meta) {
        User user = AuthContext.currentUser();
        UserSession session = AuthContext.currentSession();

        boolean ok = webAuthnService.finishAuthentication(user, credentialId, meta);
        if (ok) {
            sessionService.markStepUpVerified(session);
            auditService.log(AuditEventType.STEP_UP_MFA_SUCCESS, Severity.MEDIUM, user, meta,
                    session.getRiskScore(), "Step-up MFA verified");
            return true;
        }

        auditService.log(AuditEventType.STEP_UP_MFA_FAILED, Severity.HIGH, user, meta,
                session.getRiskScore(), "Step-up MFA failed");
        notificationService.notify("FAILED_STEP_UP_MFA", Severity.HIGH, user,
                "Step-up MFA failed for " + user.getUsername() + " on a critical action");
        incidentService.create(Severity.HIGH, "Failed step-up MFA for critical action",
                user.getUsername() + " failed step-up MFA while attempting a critical CPS action.", user);
        throw ApiException.badRequest("Step-up MFA verification failed.");
    }

    /**
     * Executes a previously requested (PENDING_STEP_UP) action now that step-up is satisfied.
     * The frontend may alternatively just re-call the original HMI endpoint.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public CpsActionResult executeAction(Long actionId, RequestMeta meta) {
        UserSession session = AuthContext.currentSession();
        if (!session.stepUpValid()) {
            throw ApiException.forbidden("Step-up MFA is required before executing this action.");
        }
        CpsAction pending = actionRepository.findById(actionId)
                .orElseThrow(() -> ApiException.notFound("Pending action not found: " + actionId));
        if (pending.getStatus() != CpsActionStatus.PENDING_STEP_UP) {
            throw ApiException.badRequest("Action is not awaiting step-up.");
        }

        HmiActionType action = HmiActionType.valueOf(pending.getActionType());
        Double value = parseValue(pending.getNewValue());
        CpsActionResult result = cpsActionService.execute(action, value, meta);

        pending.setStatus(CpsActionStatus.EXECUTED);
        pending.setStepUpVerified(true);
        actionRepository.save(pending);
        return result;
    }

    private HmiActionType parse(String actionType) {
        try {
            return HmiActionType.valueOf(actionType);
        } catch (Exception e) {
            throw ApiException.badRequest("Unknown action type: " + actionType);
        }
    }

    private Double parseValue(String raw) {
        try {
            return raw == null ? null : Double.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
