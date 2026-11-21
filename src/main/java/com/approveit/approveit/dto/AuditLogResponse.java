package com.approveit.approveit.dto;

import com.approveit.approveit.model.AuditLog;

import java.time.Instant;

public class AuditLogResponse {

    private Long id;
    private String actorUserId;
    private Long paymentId;
    private String action;
    private String stateBefore;
    private String stateAfter;
    private String outcome;
    private String message;
    private Instant timestamp;

    public AuditLogResponse(AuditLog auditLog) {

        this.id = auditLog.getId();
        this.actorUserId = auditLog.getActorUserId();

        if (auditLog.getPayment() != null) {
            this.paymentId = auditLog.getPayment().getId();
        }

        this.action = auditLog.getAction();
        this.stateBefore = auditLog.getStateBefore();
        this.stateAfter = auditLog.getStateAfter();
        this.outcome = auditLog.getOutcome();
        this.message = auditLog.getMessage();
        this.timestamp = auditLog.getTimestamp();
    }

    public Long getId() {
        return id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getAction() {
        return action;
    }

    public String getStateBefore() {
        return stateBefore;
    }

    public String getStateAfter() {
        return stateAfter;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}