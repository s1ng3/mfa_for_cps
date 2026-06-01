package com.cps.mfa.stepup;

import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.SimpleResponse;
import com.cps.mfa.hmi.CpsActionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Step-up MFA endpoints used when an authenticated operator attempts a critical CPS action. */
@RestController
@RequestMapping("/api/step-up")
@RequiredArgsConstructor
public class StepUpController {

    private final StepUpService stepUpService;

    public record StepUpRequest(@NotBlank String actionType) {
    }

    public record StepUpVerifyRequest(String credentialId) {
    }

    public record ExecuteActionRequest(@NotNull Long actionId) {
    }

    @PostMapping("/request")
    public Map<String, Object> request(@RequestBody StepUpRequest req, HttpServletRequest http) {
        return stepUpService.request(req.actionType(), RequestMeta.from(http));
    }

    @PostMapping("/verify")
    public SimpleResponse verify(@RequestBody StepUpVerifyRequest req, HttpServletRequest http) {
        stepUpService.verify(req.credentialId(), RequestMeta.from(http));
        return SimpleResponse.ok("Step-up MFA verified. You may perform the critical action.");
    }

    @PostMapping("/execute-action")
    public CpsActionResult execute(@RequestBody ExecuteActionRequest req, HttpServletRequest http) {
        return stepUpService.executeAction(req.actionId(), RequestMeta.from(http));
    }
}
