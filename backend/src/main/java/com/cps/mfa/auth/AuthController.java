package com.cps.mfa.auth;

import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.SimpleResponse;
import com.cps.mfa.session.AuthContext;
import com.cps.mfa.session.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Password stage + session lifecycle endpoints. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, RequestMeta.from(http));
    }

    @PostMapping("/logout")
    public SimpleResponse logout() {
        authService.logout(AuthContext.currentSession());
        return SimpleResponse.ok("Logged out");
    }

    @GetMapping("/me")
    public MeResponse me() {
        AuthPrincipal principal = AuthContext.current();
        return MeResponse.from(principal.user(), principal.session());
    }
}
