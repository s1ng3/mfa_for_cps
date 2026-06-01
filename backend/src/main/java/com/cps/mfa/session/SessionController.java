package com.cps.mfa.session;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the logged-in operator inspect their own current session (used by the session timer UI). */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    @GetMapping("/current")
    public SessionDto current() {
        return SessionDto.from(AuthContext.currentSession());
    }
}
