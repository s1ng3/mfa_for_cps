package com.cps.mfa.hmi;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.*;
import com.cps.mfa.notification.NotificationService;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.session.AuthContext;
import com.cps.mfa.session.UserSession;
import com.cps.mfa.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes HMI control actions through the full security pipeline:
 * RBAC check → critical-action / step-up gate → simulator mutation → audit + notification.
 */
@Service
@RequiredArgsConstructor
public class CpsActionService {

    private final ProcessSimulatorService simulator;
    private final CriticalActionService criticalActionService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final CpsActionRepository actionRepository;

    // noRollbackFor: an UNAUTHORIZED_ACTION_ATTEMPT audit/alert raised by the RBAC check must
    // persist even though require() then throws 403.
    @Transactional(noRollbackFor = ApiException.class)
    public CpsActionResult execute(HmiActionType action, Double value, RequestMeta meta) {
        User user = AuthContext.currentUser();
        UserSession session = AuthContext.currentSession();

        // 1. Role-based access control (throws + audits UNAUTHORIZED_ACTION_ATTEMPT on denial).
        authorizationService.require(user, action.permission(), meta, action.name());

        // 2. Step-up gate for critical actions.
        if (criticalActionService.requiresStepUp(action, session)) {
            auditService.log(AuditEventType.CRITICAL_ACTION_REQUESTED, Severity.MEDIUM, user, meta,
                    session.getRiskScore(), "Critical action requested: " + action.name());
            auditService.log(AuditEventType.STEP_UP_MFA_REQUIRED, Severity.MEDIUM, user, meta,
                    session.getRiskScore(), "Step-up MFA required for " + action.name());
            CpsAction pending = record(user, action, null,
                    value == null ? null : String.valueOf(value), true, false, CpsActionStatus.PENDING_STEP_UP);
            return new CpsActionResult(false, true, pending.getId(),
                    "Step-up MFA required for " + action.name(), simulator.status());
        }

        // 3. Execute against the simulator.
        String oldValue = applyToSimulator(action, value);
        String newValue = newValueFor(action, value);

        CpsAction executed = record(user, action, oldValue, newValue,
                action.critical(), action.critical(), CpsActionStatus.EXECUTED);

        auditService.log(AuditEventType.CPS_ACTION_EXECUTED,
                action.critical() ? Severity.MEDIUM : Severity.LOW, user, meta, session.getRiskScore(),
                action.name() + " on " + action.component() + " [" + oldValue + " -> " + newValue + "]");

        if (action.critical()) {
            notificationService.notify("CRITICAL_CPS_ACTION", Severity.MEDIUM, user,
                    user.getUsername() + " executed critical action " + action.name()
                            + " (" + oldValue + " -> " + newValue + ")");
        }

        return new CpsActionResult(true, false, executed.getId(),
                action.name() + " executed.", simulator.status());
    }

    private String applyToSimulator(HmiActionType action, Double value) {
        return switch (action) {
            case START_PUMP -> simulator.startPump();
            case STOP_PUMP -> simulator.stopPump();
            case ACKNOWLEDGE_ALARM -> simulator.acknowledgeAlarm();
            case RESET_EMERGENCY_STOP -> simulator.resetEmergencyStop();
            case CHANGE_MOTOR_SPEED -> simulator.setMotorSpeed(require(value, action));
            case CHANGE_PRESSURE_SETPOINT -> simulator.setPressureSetpoint(require(value, action));
            case CHANGE_TEMPERATURE_SETPOINT -> simulator.setTemperatureSetpoint(require(value, action));
        };
    }

    private String newValueFor(HmiActionType action, Double value) {
        return switch (action) {
            case START_PUMP -> "true";
            case STOP_PUMP -> "false";
            case ACKNOWLEDGE_ALARM -> "acknowledged";
            case RESET_EMERGENCY_STOP -> "false";
            default -> String.valueOf(value);
        };
    }

    private double require(Double value, HmiActionType action) {
        if (value == null) {
            throw ApiException.badRequest("A numeric value is required for " + action.name());
        }
        return value;
    }

    private CpsAction record(User user, HmiActionType action, String oldValue, String newValue,
                             boolean requiresStepUp, boolean stepUpVerified, CpsActionStatus status) {
        CpsAction entity = new CpsAction();
        entity.setUserId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setActionType(action.name());
        entity.setTargetComponent(action.component());
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        entity.setRequiresStepUp(requiresStepUp);
        entity.setStepUpVerified(stepUpVerified);
        entity.setStatus(status);
        return actionRepository.save(entity);
    }
}
