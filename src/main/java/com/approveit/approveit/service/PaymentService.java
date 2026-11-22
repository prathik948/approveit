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


    //Submit
    @Transactional
    public PaymentResponse submitPayment(Long paymentId, String userId) {

        Payment payment = null;
        String stateBefore = null;

        try {

            // Find payment
            payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() ->
                            new BusinessException("Payment not found"));

            stateBefore = payment.getStatus().name();

            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new BusinessException("User not found"));

            // Check Maker role
            if (!user.getRoles().contains(Role.MAKER)) {
                throw new BusinessException("Only a Maker can submit payments");
            }

            // Check original Maker
            if (!payment.getMaker().getUserId().equals(userId)) {
                throw new BusinessException(
                        "Only the original Maker can submit this payment");
            }

            // Check payment status
            if (payment.getStatus() != PaymentStatus.DRAFT) {
                throw new BusinessException(
                        "Only DRAFT payments can be submitted");
            }

            // Get today's date using the injectable business clock
            ZoneId ist = ZoneId.of("Asia/Kolkata");
            LocalDate today = LocalDate.now(businessClock);

            Instant startOfDay = today.atStartOfDay(ist).toInstant();
            Instant startOfNextDay = today.plusDays(1)
                    .atStartOfDay(ist).toInstant();

            // Get already submitted amount for today
            BigDecimal alreadySubmitted =
                    paymentRepository.getDailySubmittedAmount(
                            userId, startOfDay, startOfNextDay);

            // Daily limit: 50 lakh
            BigDecimal dailyLimit = new BigDecimal("5000000.00");

            BigDecimal newTotal = alreadySubmitted.add(payment.getAmount());

            if (newTotal.compareTo(dailyLimit) > 0) {
                throw new BusinessException(
                        "Daily submission limit of Rs. 50,00,000 exceeded");
            }

            // Submit payment
            payment.setStatus(PaymentStatus.SUBMITTED);
            payment.setSubmittedAt(Instant.now(businessClock));

            Payment submittedPayment = paymentRepository.save(payment);

            // Record successful submission
            auditLogService.logAction(
                    userId,
                    submittedPayment,
                    "SUBMIT_PAYMENT",
                    stateBefore,
                    submittedPayment.getStatus().name(),
                    "SUCCESS",
                    "Payment submitted successfully"
            );

            return new PaymentResponse(submittedPayment);

        } catch (BusinessException ex) {

            // Record failed submission
            auditLogService.logAction(
                    userId,
                    payment != null ? payment.getId() : paymentId,
                    "SUBMIT_PAYMENT",
                    stateBefore,
                    stateBefore,
                    "FAILURE",
                    ex.getMessage()
            );

            throw ex;
        }
    }


    //Approve
    @Transactional
    public PaymentResponse approvePayment(Long paymentId, String userId) {

        Payment payment = null;
        String stateBefore = null;

        try {

            // Find and lock payment
            payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() ->
                            new BusinessException("Payment not found"));

            stateBefore = payment.getStatus().name();

            // Find Checker
            User checker = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new BusinessException("User not found"));

            // Check Checker role
            if (!checker.getRoles().contains(Role.CHECKER)) {
                throw new BusinessException(
                        "Only a Checker can approve payments");
            }

            // Prevent self-approval
            if (payment.getMaker().getUserId().equals(userId)) {
                throw new BusinessException(
                        "Maker cannot approve their own payment");
            }

            // Check payment status
            if (payment.getStatus() != PaymentStatus.SUBMITTED) {
                throw new BusinessException(
                        "Only SUBMITTED payments can be approved");
            }

            // Check duplicate approval
            boolean alreadyApproved =
                    approvalRepository.existsByPayment_IdAndChecker_UserId(
                            paymentId, userId);

            if (alreadyApproved) {
                throw new BusinessException(
                        "This Checker has already approved this payment");
            }

            // Save approval
            Approval approval = new Approval();
            approval.setPayment(payment);
            approval.setChecker(checker);
            approval.setDecision(ApprovalDecision.APPROVED);

            approvalRepository.save(approval);

            // Determine required approvals
            BigDecimal approvalLimit = new BigDecimal("1000000.00");

            long requiredApprovals =
                    payment.getAmount().compareTo(approvalLimit) > 0 ? 2 : 1;

            // Count existing approvals
            long totalApprovals =
                    approvalRepository.countByPayment_IdAndDecision(
                            paymentId, ApprovalDecision.APPROVED);

            // Update status when approval threshold is reached
            if (totalApprovals >= requiredApprovals) {
                payment.setStatus(PaymentStatus.APPROVED);
            }

            // Save updated payment
            Payment approvedPayment = paymentRepository.save(payment);

            // Record successful approval
            auditLogService.logAction(
                    userId,
                    approvedPayment,
                    "APPROVE_PAYMENT",
                    stateBefore,
                    approvedPayment.getStatus().name(),
                    "SUCCESS",
                    "Payment approval recorded successfully"
            );

            return new PaymentResponse(approvedPayment);

        } catch (BusinessException ex) {

            // Record failed approval
            auditLogService.logAction(
                    userId,
                    payment != null ? payment.getId() : paymentId,
                    "APPROVE_PAYMENT",
                    stateBefore,
                    stateBefore,
                    "FAILURE",
                    ex.getMessage()
            );

            throw ex;
        }
    }


    //Reject

    @Transactional
    public PaymentResponse rejectPayment(
            Long paymentId,
            String userId,
            RejectPaymentRequest request) {

        Payment payment = null;
        String stateBefore = null;

        try {
            // Validate rejection reason
            if (request == null
                    || request.getReason() == null
                    || request.getReason().isBlank()) {

                throw new BusinessException(
                        "Rejection reason is required");
            }

            // Find and lock payment
            payment = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() ->
                            new BusinessException("Payment not found"));

            stateBefore = payment.getStatus().name();

            // Find Checker
            User checker = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new BusinessException("User not found"));

            // Check Checker role
            if (!checker.getRoles().contains(Role.CHECKER)) {
                throw new BusinessException(
                        "Only a Checker can reject payments");
            }

            // Prevent self-rejection
            if (payment.getMaker().getUserId().equals(userId)) {
                throw new BusinessException(
                        "Maker cannot reject their own payment");
            }

            // Check payment status
            if (payment.getStatus() != PaymentStatus.SUBMITTED) {
                throw new BusinessException(
                        "Only SUBMITTED payments can be rejected");
            }

            // Create rejection record
            Approval rejection = new Approval();
            rejection.setPayment(payment);
            rejection.setChecker(checker);
            rejection.setDecision(ApprovalDecision.REJECTED);
            rejection.setReason(request.getReason());

            approvalRepository.save(rejection);

            // Update payment status
            payment.setStatus(PaymentStatus.REJECTED);

            Payment rejectedPayment = paymentRepository.save(payment);

            // Record successful rejection
            auditLogService.logAction(
                    userId,
                    rejectedPayment,
                    "REJECT_PAYMENT",
                    stateBefore,
                    rejectedPayment.getStatus().name(),
                    "SUCCESS",
                    "Payment rejected. Reason: " + request.getReason()
            );

            return new PaymentResponse(rejectedPayment);

        } catch (BusinessException ex) {

            // Record failed rejection attempt
            auditLogService.logAction(
                    userId,
                    payment != null ? payment.getId() : paymentId,
                    "REJECT_PAYMENT",
                    stateBefore,
                    stateBefore,
                    "FAILURE",
                    ex.getMessage()
            );

            throw ex;
        }
    }




}



