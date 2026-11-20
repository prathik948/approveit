package com.approveit.approveit.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreatePaymentRequest {

    @NotBlank(message = "Beneficiary name is required")
    private String beneficiaryName;

    @NotBlank(message = "Beneficiary ID is required")
    private String beneficiaryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false,
            message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2,
            message = "Amount can have maximum two decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

