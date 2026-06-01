package com.cps.mfa.siem;

import com.cps.mfa.audit.AuditLog;
import com.cps.mfa.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Renders the audit trail as RFC-4180 CSV for spreadsheet review or SIEM CSV ingestion. */
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final String HEADER =
            "id,timestamp,event_type,severity,user_id,username,ip_address,device_fingerprint,risk_score,details";

    private final AuditLogRepository repository;

    public String export() {
        StringBuilder sb = new StringBuilder(HEADER).append("\n");
        for (AuditLog log : repository.findAllByOrderByCreatedAtDesc()) {
            sb.append(log.getId()).append(',')
                    .append(q(log.getCreatedAt())).append(',')
                    .append(q(log.getEventType())).append(',')
                    .append(q(log.getSeverity())).append(',')
                    .append(log.getUserId() == null ? "" : log.getUserId()).append(',')
                    .append(q(log.getUsername())).append(',')
                    .append(q(log.getIpAddress())).append(',')
                    .append(q(log.getDeviceFingerprint())).append(',')
                    .append(log.getRiskScore() == null ? "" : log.getRiskScore()).append(',')
                    .append(q(log.getDetails()))
                    .append('\n');
        }
        return sb.toString();
    }

    /** Quotes a field and escapes embedded quotes per RFC 4180. */
    private String q(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString().replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
