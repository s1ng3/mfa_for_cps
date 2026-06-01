package com.cps.mfa.hmi;

import com.cps.mfa.session.UserSession;
import org.springframework.stereotype.Service;

/** Decides whether a given action is critical and whether the session has satisfied step-up MFA. */
@Service
public class CriticalActionService {

    public boolean isCritical(HmiActionType action) {
        return action.critical();
    }

    /** Critical actions require a recent step-up MFA verification on the session. */
    public boolean requiresStepUp(HmiActionType action, UserSession session) {
        return action.critical() && !session.stepUpValid();
    }
}
