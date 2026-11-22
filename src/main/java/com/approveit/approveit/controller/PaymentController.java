package com.approveit.approveit.controller;

import com.approveit.approveit.dto.CreatePaymentRequest;
import com.approveit.approveit.dto.RejectPaymentRequest;
import com.approveit.approveit.exception.BusinessException;
import com.approveit.approveit.model.Payment;
import com.approveit.approveit.repository.PaymentRepository;
import com.approveit.approveit.service.PaymentService;
import com.approveit.approveit.dto.PaymentResponse;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create a new payment
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment =
                paymentService.createPayment(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(payment);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<PaymentResponse> submitPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        PaymentResponse submittedPayment =
                paymentService.submitPayment(id, userId);

        return ResponseEntity.ok(submittedPayment);
    }



    @PutMapping("/{id}/approve")
    public ResponseEntity<PaymentResponse> approvePayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        PaymentResponse approvedPayment =
                paymentService.approvePayment(id, userId);

        return ResponseEntity.ok(approvedPayment);
    }



    @PutMapping("/{id}/reject")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RejectPaymentRequest request) {

        PaymentResponse rejectedPayment =
                paymentService.rejectPayment(id, userId, request);

        return ResponseEntity.ok(rejectedPayment);
    }



    @PutMapping("/{id}/release")
    public ResponseEntity<PaymentResponse> releasePayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        PaymentResponse releasedPayment =
                paymentService.releasePayment(id, userId);

        return ResponseEntity.ok(releasedPayment);
    }




}

