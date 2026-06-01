package com.cps.mfa.rbac;

/** Catalogue of permission names enforced across the gateway. */
public final class Permissions {
    // HMI / CPS
    public static final String HMI_VIEW = "HMI_VIEW";
    public static final String HMI_START_PUMP = "HMI_START_PUMP";
    public static final String HMI_STOP_PUMP = "HMI_STOP_PUMP";
    public static final String HMI_ACK_ALARM = "HMI_ACK_ALARM";
    public static final String HMI_CHANGE_MOTOR_SPEED = "HMI_CHANGE_MOTOR_SPEED";
    public static final String HMI_CHANGE_PRESSURE = "HMI_CHANGE_PRESSURE_SETPOINT";
    public static final String HMI_CHANGE_TEMPERATURE = "HMI_CHANGE_TEMPERATURE_SETPOINT";
    public static final String HMI_RESET_ESTOP = "HMI_RESET_EMERGENCY_STOP";

    // Administration
    public static final String ADMIN_MANAGE_USERS = "ADMIN_MANAGE_USERS";
    public static final String ADMIN_VIEW_SESSIONS = "ADMIN_VIEW_SESSIONS";
    public static final String ADMIN_TERMINATE_SESSIONS = "ADMIN_TERMINATE_SESSIONS";

    // Security operations
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String INCIDENT_VIEW = "INCIDENT_VIEW";
    public static final String INCIDENT_MANAGE = "INCIDENT_MANAGE";
    public static final String SIEM_EXPORT = "SIEM_EXPORT";

    private Permissions() {
    }
}
