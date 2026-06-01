package com.cps.mfa.hmi;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A tiny water-treatment process simulator standing in for a real HMI/SCADA backend. A scheduled
 * tick advances the physical state; control actions mutate setpoints/actuators. All access is
 * synchronised because the scheduler thread and request threads both touch the state.
 */
@Service
public class ProcessSimulatorService {

    // Actuators / setpoints
    private boolean pumpRunning = false;
    private double motorSpeed = 0;            // RPM (0..3000)
    private boolean emergencyStop = false;
    private double temperatureSetpoint = 55;  // deg C
    private double pressureSetpoint = 5.0;    // bar

    // Process variables
    private double tankLevel = 45;            // %
    private double waterTemperature = 22;     // deg C
    private double pressure = 1.0;            // bar

    // Alarm state
    private boolean alarmActive = false;
    private String alarmMessage = "";
    private boolean alarmAcknowledged = false;

    /** Advances the simulation roughly every 2 seconds. */
    @Scheduled(fixedRate = 2000)
    public synchronized void tick() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (emergencyStop) {
            pumpRunning = false;
            motorSpeed = 0;
        }

        if (pumpRunning) {
            tankLevel += 1.2 + rnd.nextDouble(-0.3, 0.6);
            pressure += (pressureSetpoint - pressure) * 0.15 + rnd.nextDouble(-0.05, 0.1);
            double heating = (motorSpeed / 3000.0) * 4.0;
            waterTemperature += (temperatureSetpoint - waterTemperature) * 0.08 + heating * 0.1;
        } else {
            tankLevel -= 0.8 + rnd.nextDouble(-0.2, 0.3);
            pressure += (1.0 - pressure) * 0.2;
            waterTemperature += (22 - waterTemperature) * 0.05;
        }

        tankLevel = clamp(tankLevel, 0, 100);
        pressure = clamp(pressure, 0, 12);
        waterTemperature = clamp(waterTemperature, 0, 120);

        evaluateAlarms();
    }

    private void evaluateAlarms() {
        String msg = null;
        if (tankLevel > 90) {
            msg = "HIGH TANK LEVEL (" + round(tankLevel) + "%)";
        } else if (tankLevel < 10) {
            msg = "LOW TANK LEVEL (" + round(tankLevel) + "%)";
        } else if (pressure > pressureSetpoint * 1.6) {
            msg = "OVER-PRESSURE (" + round(pressure) + " bar)";
        } else if (waterTemperature > temperatureSetpoint + 15) {
            msg = "OVER-TEMPERATURE (" + round(waterTemperature) + " C)";
        }

        if (msg != null) {
            if (!alarmActive || !msg.equals(alarmMessage)) {
                alarmAcknowledged = false; // a new/changed alarm needs fresh acknowledgement
            }
            alarmActive = true;
            alarmMessage = msg;
        } else if (alarmActive && alarmAcknowledged) {
            alarmActive = false;
            alarmMessage = "";
        }
    }

    public synchronized HmiStatus status() {
        return new HmiStatus(round(tankLevel), round(waterTemperature), round(pressure),
                pumpRunning, round(motorSpeed), alarmActive, alarmMessage, emergencyStop,
                round(temperatureSetpoint), round(pressureSetpoint), Instant.now());
    }

    // ---- Control actions (return the previous value as a string for the audit trail) -------

    public synchronized String startPump() {
        if (emergencyStop) {
            throw com.cps.mfa.common.ApiException.badRequest("Cannot start pump while E-STOP is engaged.");
        }
        String old = String.valueOf(pumpRunning);
        pumpRunning = true;
        return old;
    }

    public synchronized String stopPump() {
        String old = String.valueOf(pumpRunning);
        pumpRunning = false;
        return old;
    }

    public synchronized String acknowledgeAlarm() {
        String old = String.valueOf(alarmActive);
        alarmAcknowledged = true;
        return old;
    }

    public synchronized String setMotorSpeed(double rpm) {
        String old = String.valueOf(round(motorSpeed));
        motorSpeed = clamp(rpm, 0, 3000);
        return old;
    }

    public synchronized String setTemperatureSetpoint(double value) {
        String old = String.valueOf(round(temperatureSetpoint));
        temperatureSetpoint = clamp(value, 0, 95);
        return old;
    }

    public synchronized String setPressureSetpoint(double value) {
        String old = String.valueOf(round(pressureSetpoint));
        pressureSetpoint = clamp(value, 0, 10);
        return old;
    }

    public synchronized String resetEmergencyStop() {
        String old = String.valueOf(emergencyStop);
        emergencyStop = false;
        return old;
    }

    public synchronized String engageEmergencyStop() {
        String old = String.valueOf(emergencyStop);
        emergencyStop = true;
        pumpRunning = false;
        motorSpeed = 0;
        return old;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
