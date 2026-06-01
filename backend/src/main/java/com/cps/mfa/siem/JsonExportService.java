package com.cps.mfa.siem;

import com.cps.mfa.audit.AuditDto;
import com.cps.mfa.audit.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Serialises the audit trail as a JSON array suitable for SIEM ingestion (e.g. Splunk/ELK). */
@Service
@RequiredArgsConstructor
public class JsonExportService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public String export() {
        List<AuditDto> events = repository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditDto::from).toList();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(events);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise audit events", e);
        }
    }
}
