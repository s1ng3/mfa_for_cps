package com.cps.mfa.audit;

import com.cps.mfa.common.ApiException;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.rbac.Permissions;
import com.cps.mfa.session.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only audit log access for ADMIN and SECURITY_OFFICER roles. */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repository;
    private final AuthorizationService authorizationService;

    @GetMapping("/logs")
    public List<AuditDto> logs(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "100") int size,
                               HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.AUDIT_VIEW,
                RequestMeta.from(http), "VIEW_AUDIT_LOGS");
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(AuditDto::from).getContent();
    }

    @GetMapping("/logs/{id}")
    public AuditDto log(@PathVariable Long id, HttpServletRequest http) {
        authorizationService.require(AuthContext.currentUser(), Permissions.AUDIT_VIEW,
                RequestMeta.from(http), "VIEW_AUDIT_LOG");
        return repository.findById(id).map(AuditDto::from)
                .orElseThrow(() -> ApiException.notFound("Audit log not found: " + id));
    }
}
