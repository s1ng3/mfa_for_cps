package com.cps.mfa.hmi;

import com.cps.mfa.rbac.Permissions;

/**
 * Catalogue of HMI control actions. Each carries the permission that guards it, whether it is a
 * critical action (requiring step-up MFA), and the component it targets.
 */
public enum HmiActionType {
    START_PUMP(Permissions.HMI_START_PUMP, false, "PUMP"),
    STOP_PUMP(Permissions.HMI_STOP_PUMP, false, "PUMP"),
    ACKNOWLEDGE_ALARM(Permissions.HMI_ACK_ALARM, false, "ALARM"),
    CHANGE_MOTOR_SPEED(Permissions.HMI_CHANGE_MOTOR_SPEED, true, "MOTOR"),
    CHANGE_PRESSURE_SETPOINT(Permissions.HMI_CHANGE_PRESSURE, true, "PRESSURE"),
    CHANGE_TEMPERATURE_SETPOINT(Permissions.HMI_CHANGE_TEMPERATURE, true, "TEMPERATURE"),
    RESET_EMERGENCY_STOP(Permissions.HMI_RESET_ESTOP, true, "E_STOP");

    private final String permission;
    private final boolean critical;
    private final String component;

    HmiActionType(String permission, boolean critical, String component) {
        this.permission = permission;
        this.critical = critical;
        this.component = component;
    }

    public String permission() {
        return permission;
    }

    public boolean critical() {
        return critical;
    }

    public String component() {
        return component;
    }
}
