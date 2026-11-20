package com.approveit.approveit.dto;

import com.approveit.approveit.enums.PaymentStatus;
import com.approveit.approveit.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {

    private Long id;
    private String beneficiaryName;
    private String beneficiaryId;
    private BigDecimal amount;
    private String purpose;
    private String makerId;
    private String makerName;
    private PaymentStatus status;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant updatedAt;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.beneficiaryName = payment.getBeneficiaryName();
        this.beneficiaryId = payment.getBeneficiaryId();
        this.amount = payment.getAmount();
        this.purpose = payment.getPurpose();
        this.makerId = payment.getMaker().getUserId();
        this.makerName = payment.getMaker().getName();
        this.status = payment.getStatus();
        this.createdAt = payment.getCreatedAt();
        this.submittedAt = payment.getSubmittedAt();
        this.updatedAt = payment.getUpdatedAt();
    }

    public Long getId() { return id; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryId() { return beneficiaryId; }
    public BigDecimal getAmount() { return amount; }
    public String getPurpose() { return purpose; }
    public String getMakerId() { return makerId; }
    public String getMakerName() { return makerName; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
