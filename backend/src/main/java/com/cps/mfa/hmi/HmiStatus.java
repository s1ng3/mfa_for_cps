package com.cps.mfa.hmi;

import java.time.Instant;

/** Snapshot of the simulated CPS/HMI process values shown on the operator dashboard. */
public record HmiStatus(
        double tankLevel,          // %
        double waterTemperature,   // deg C
        double pressure,           // bar
        boolean pumpRunning,
        double motorSpeed,         // RPM
        boolean alarmActive,
        String alarmMessage,
        boolean emergencyStop,
        double temperatureSetpoint,
        double pressureSetpoint,
        Instant timestamp
) {
}
