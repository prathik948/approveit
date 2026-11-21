package com.approveit.approveit.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")

public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Store the actor ID even for unknown or invalid users
    @Column(nullable = false, updatable = false)
    private String actorUserId;

    // Payment can be null for actions before payment creation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(updatable = false)
    private String stateBefore;

    @Column(updatable = false)
    private String stateAfter;

    @Column(nullable = false, updatable = false)
    private String outcome;

    @Column(length = 1000, updatable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    public AuditLog() {
    }

    @PrePersist
    protected void onCreate() {
        timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStateBefore() {
        return stateBefore;
    }

    public void setStateBefore(String stateBefore) {
        this.stateBefore = stateBefore;
    }

    public String getStateAfter() {
        return stateAfter;
    }

    public void setStateAfter(String stateAfter) {
        this.stateAfter = stateAfter;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
