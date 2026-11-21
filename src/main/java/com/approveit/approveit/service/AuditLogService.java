
package com.approveit.approveit.service;

import com.approveit.approveit.model.AuditLog;
import com.approveit.approveit.model.Payment;
import com.approveit.approveit.repository.AuditLogRepository;
import com.approveit.approveit.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PaymentRepository paymentRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            PaymentRepository paymentRepository) {

        this.auditLogRepository = auditLogRepository;
        this.paymentRepository = paymentRepository;
    }

    // Existing method: for successful operations
    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(
            String userId,
            Payment payment,
            String action,
            String stateBefore,
            String stateAfter,
            String outcome,
            String message) {

        saveAuditLog(
                userId, payment, action,
                stateBefore, stateAfter, outcome, message
        );
    }

    // New method: for failed operations
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            String userId,
            Long paymentId,
            String action,
            String stateBefore,
            String stateAfter,
            String outcome,
            String message) {

        Payment payment = null;

        if (paymentId != null) {
            payment = paymentRepository.getReferenceById(paymentId);
        }

        saveAuditLog(
                userId, payment, action,
                stateBefore, stateAfter, outcome, message
        );
    }

    private void saveAuditLog(
            String userId,
            Payment payment,
            String action,
            String stateBefore,
            String stateAfter,
            String outcome,
            String message) {

        AuditLog auditLog = new AuditLog();

        auditLog.setActorUserId(userId);
        auditLog.setPayment(payment);
        auditLog.setAction(action);
        auditLog.setStateBefore(stateBefore);
        auditLog.setStateAfter(stateAfter);
        auditLog.setOutcome(outcome);
        auditLog.setMessage(message);

        auditLogRepository.save(auditLog);
    }
}
