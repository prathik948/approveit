package com.approveit.approveit.model;

import com.approveit.approveit.enums.ApprovalDecision;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "approvals",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_checker",
                        columnNames = {"payment_id", "checker_id"}
                )
        }
)

public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checker_id", nullable = false)
    private User checker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Column(nullable = false)
    private Instant decisionAt;

    private String reason;

    public Approval() {
    }

    @PrePersist
    protected void onCreate() {
        decisionAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public User getChecker() {
        return checker;
    }

    public void setChecker(User checker) {
        this.checker = checker;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecision decision) {
        this.decision = decision;
    }

    public Instant getDecisionAt() {
        return decisionAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
