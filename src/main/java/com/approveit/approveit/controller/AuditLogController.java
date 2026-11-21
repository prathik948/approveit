package com.approveit.approveit.controller;

import com.approveit.approveit.dto.AuditLogResponse;
import com.approveit.approveit.model.AuditLog;
import com.approveit.approveit.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLogResponse> getAllAuditLogs() {

        List<AuditLog> auditLogs = auditLogRepository.findAll();

        List<AuditLogResponse> responses = new ArrayList<>();

        for (AuditLog auditLog : auditLogs) {
            responses.add(new AuditLogResponse(auditLog));
        }

        return responses;
    }
}