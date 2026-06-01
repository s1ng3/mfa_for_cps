package com.cps.mfa.siem;

import com.cps.mfa.audit.AuditService;
import com.cps.mfa.common.AuditEventType;
import com.cps.mfa.common.RequestMeta;
import com.cps.mfa.common.Severity;
import com.cps.mfa.rbac.AuthorizationService;
import com.cps.mfa.rbac.Permissions;
import com.cps.mfa.session.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** SIEM-style structured log export (JSON / CSV). Restricted to SIEM_EXPORT holders. */
@RestController
@RequestMapping("/api/siem")
@RequiredArgsConstructor
public class SiemExportController {

    private final JsonExportService jsonExportService;
    private final CsvExportService csvExportService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(HttpServletRequest http) {
        authorize(http, "EXPORT_SIEM_JSON");
        byte[] body = jsonExportService.export().getBytes(StandardCharsets.UTF_8);
        record(http, "JSON");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"siem-audit-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(HttpServletRequest http) {
        authorize(http, "EXPORT_SIEM_CSV");
        byte[] body = csvExportService.export().getBytes(StandardCharsets.UTF_8);
        record(http, "CSV");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"siem-audit-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    private void authorize(HttpServletRequest http, String action) {
        authorizationService.require(AuthContext.currentUser(), Permissions.SIEM_EXPORT,
                RequestMeta.from(http), action);
    }

    private void record(HttpServletRequest http, String format) {
        auditService.log(AuditEventType.LOG_EXPORTED, Severity.MEDIUM, AuthContext.currentUser(),
                RequestMeta.from(http), null, "SIEM audit export (" + format + ")");
    }
}
