package com.cps.mfa.hmi;

import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.rbac.Permissions;
import com.cps.mfa.session.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * HMI/SCADA endpoints. Reads require HMI_VIEW; non-critical controls require the matching role
 * permission; critical controls additionally require step-up MFA (signalled via the response).
 */
@RestController
@RequestMapping("/api/hmi")
@RequiredArgsConstructor
public class HmiController {

    private final ProcessSimulatorService simulator;
    private final CpsActionService cpsActionService;
    private final AuthorizationService authorizationService;

    /** Numeric payload for setpoint/speed changes. */
    public record NumericValueRequest(double value) {
    }

    @GetMapping("/status")
    public HmiStatus status(HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.HMI_VIEW,
                RequestMeta.from(http), "VIEW_HMI");
        return simulator.status();
    }

    @PostMapping("/start-pump")
    public CpsActionResult startPump(HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.START_PUMP, null, RequestMeta.from(http));
    }

    @PostMapping("/stop-pump")
    public CpsActionResult stopPump(HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.STOP_PUMP, null, RequestMeta.from(http));
    }

    @PostMapping("/acknowledge-alarm")
    public CpsActionResult acknowledgeAlarm(HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.ACKNOWLEDGE_ALARM, null, RequestMeta.from(http));
    }

    @PostMapping("/change-motor-speed")
    public CpsActionResult changeMotorSpeed(@RequestBody NumericValueRequest req, HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.CHANGE_MOTOR_SPEED, req.value(), RequestMeta.from(http));
    }

    @PostMapping("/change-temperature-setpoint")
    public CpsActionResult changeTemperature(@RequestBody NumericValueRequest req, HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.CHANGE_TEMPERATURE_SETPOINT, req.value(), RequestMeta.from(http));
    }

    @PostMapping("/change-pressure-setpoint")
    public CpsActionResult changePressure(@RequestBody NumericValueRequest req, HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.CHANGE_PRESSURE_SETPOINT, req.value(), RequestMeta.from(http));
    }

    @PostMapping("/reset-emergency-stop")
    public CpsActionResult resetEmergencyStop(HttpServletRequest http) {
        return cpsActionService.execute(HmiActionType.RESET_EMERGENCY_STOP, null, RequestMeta.from(http));
    }
}
