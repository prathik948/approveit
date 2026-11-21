package com.approveit.approveit.service;

import com.approveit.approveit.dto.CreatePaymentRequest;
import com.approveit.approveit.enums.ApprovalDecision;
import com.approveit.approveit.enums.PaymentStatus;
import com.approveit.approveit.enums.Role;
import com.approveit.approveit.model.AuditLog;
import com.approveit.approveit.model.Payment;
import com.approveit.approveit.model.User;
import com.approveit.approveit.repository.ApprovalRepository;
import com.approveit.approveit.repository.AuditLogRepository;
import com.approveit.approveit.repository.PaymentRepository;
import com.approveit.approveit.repository.UserRepository;
import com.approveit.approveit.exception.BusinessException;
import java.util.List;
import java.time.Clock;
import java.util.ArrayList;
import com.approveit.approveit.dto.PaymentResponse;
import java.util.stream.Collectors;
import com.approveit.approveit.repository.ApprovalRepository;
import com.approveit.approveit.dto.RejectPaymentRequest;
import com.approveit.approveit.enums.ApprovalDecision;
import com.approveit.approveit.model.Approval;
import com.approveit.approveit.service.AuditLogService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.math.BigDecimal;

import com.approveit.approveit.model.Approval;
import com.approveit.approveit.enums.ApprovalDecision;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogService auditLogService;
    private final Clock businessClock;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository,
                          ApprovalRepository approvalRepository,
                          AuditLogService auditLogService,
                          Clock businessClock) {

        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.approvalRepository = approvalRepository;
        this.auditLogService = auditLogService;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {

        List<Payment> payments = paymentRepository.findAll();

        List<PaymentResponse> responses = new ArrayList<>();

        for (Payment payment : payments) {
            PaymentResponse response = new PaymentResponse(payment);
            responses.add(response);
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        return new PaymentResponse(payment);
    }

    //Create
    @Transactional
    public Payment createPayment(String userId,
                                 CreatePaymentRequest request) {

        // 1. Find the user
        User maker = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException("User not found"));

        // 2. Check whether the user has Maker role
        if (!maker.getRoles().contains(Role.MAKER)) {
            throw new BusinessException(
                    "Only a Maker can create payments");
        }

        // 3. Create a new Payment object
        Payment payment = new Payment();

        payment.setBeneficiaryName(request.getBeneficiaryName());
        payment.setBeneficiaryId(request.getBeneficiaryId());
        payment.setAmount(request.getAmount());
        payment.setPurpose(request.getPurpose());

        payment.setMaker(maker);

        // 4. Set initial status
        payment.setStatus(PaymentStatus.DRAFT);

        // 5. Save payment in database
        Payment savedPayment = paymentRepository.save(payment);

        // 6. Record successful action in audit log
        // 6. Record successful action using AuditLogService
        auditLogService.logAction(
                userId,
                savedPayment,
                "CREATE_PAYMENT",
                "NONE",
                "DRAFT",
                "SUCCESS",
                "Payment created successfully"
        );

        return savedPayment;
    }



}



