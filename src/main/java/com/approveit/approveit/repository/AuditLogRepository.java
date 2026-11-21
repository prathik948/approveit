package com.approveit.approveit.repository;

import com.approveit.approveit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}

