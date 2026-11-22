package com.approveit.approveit;

import com.approveit.approveit.dto.CreatePaymentRequest;
import com.approveit.approveit.enums.ApprovalDecision;
import com.approveit.approveit.exception.BusinessException;
import com.approveit.approveit.model.Payment;
import com.approveit.approveit.repository.ApprovalRepository;
import com.approveit.approveit.service.PaymentService;
import com.approveit.approveit.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.approveit.approveit.dto.PaymentResponse;
import com.approveit.approveit.dto.RejectPaymentRequest;
import com.approveit.approveit.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approveit_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ApprovalRepository approvalRepository;

    @Test
    void applicationStartsSuccessfully() {
        assertNotNull(paymentService);
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM audit_logs");
        jdbcTemplate.execute("DELETE FROM approvals");
        jdbcTemplate.execute("DELETE FROM payments");
    }
    @Test
    void makerCanCreatePayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST001");
        request.setAmount(new BigDecimal("25000.00"));
        request.setPurpose("Automated testing");

        Payment response =
                paymentService.createPayment("M1", request);

        assertNotNull(response);
        assertEquals("M1", response.getMaker().getUserId());
        assertEquals(PaymentStatus.DRAFT, response.getStatus());
        assertEquals(new BigDecimal("25000.00"), response.getAmount());
    }

    @Test
    void userWithoutMakerRoleCannotCreatePayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST002");
        request.setAmount(new BigDecimal("10000.00"));
        request.setPurpose("Unauthorized test");

        assertThrows(
                BusinessException.class,
                () -> paymentService.createPayment("C1", request)
        );
    }

    @Test
    void makerCannotApproveOwnPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST003");
        request.setAmount(new BigDecimal("25000.00"));
        request.setPurpose("Self approval test");

        Payment payment = paymentService.createPayment("MC1", request);

        Long paymentId = payment.getId();

        paymentService.submitPayment(paymentId, "MC1");

        assertThrows(
                BusinessException.class,
                () -> paymentService.approvePayment(paymentId, "MC1")
        );
    }

    @Test
    void oneCheckerCanApprovePaymentBelowTenLakhs() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST004");
        request.setAmount(new BigDecimal("25000.00"));
        request.setPurpose("Approval threshold test");

        Payment payment = paymentService.createPayment("M2", request);

        Long paymentId = payment.getId();

        paymentService.submitPayment(paymentId, "M2");

        paymentService.approvePayment(paymentId, "C1");

        PaymentResponse approvedPayment = paymentService.getPaymentById(paymentId);

        assertEquals(PaymentStatus.APPROVED, approvedPayment.getStatus());
    }


    @Test
    void paymentAboveTenLakhsRequiresTwoCheckers() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST005");
        request.setAmount(new BigDecimal("1500000.00"));
        request.setPurpose("Two checker approval test");

        Payment payment = paymentService.createPayment("M2", request);

        Long paymentId = payment.getId();

        paymentService.submitPayment(paymentId, "M2");

        // First checker approves
        paymentService.approvePayment(paymentId, "C1");

        PaymentResponse afterFirstApproval =
                paymentService.getPaymentById(paymentId);

        assertEquals(PaymentStatus.SUBMITTED, afterFirstApproval.getStatus());

        // Second checker approves
        paymentService.approvePayment(paymentId, "C2");

        PaymentResponse afterSecondApproval =
                paymentService.getPaymentById(paymentId);

        assertEquals(PaymentStatus.APPROVED, afterSecondApproval.getStatus());
    }

    @Test
    void sameCheckerCannotApproveTwice() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST006");
        request.setAmount(new BigDecimal("1500000.00"));
        request.setPurpose("Duplicate approval test");

        Payment payment = paymentService.createPayment("M2", request);

        Long paymentId = payment.getId();

        paymentService.submitPayment(paymentId, "M2");

        // First approval by C1
        paymentService.approvePayment(paymentId, "C1");

        // Same Checker tries to approve again
        assertThrows(
                BusinessException.class,
                () -> paymentService.approvePayment(paymentId, "C1")
        );
    }

    @Test
    void checkerCanRejectPaymentWithReason() {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST007");
        request.setAmount(new BigDecimal("50000.00"));
        request.setPurpose("Rejection test");

        Payment payment = paymentService.createPayment("M2", request);

        Long paymentId = payment.getId();

        paymentService.submitPayment(paymentId, "M2");

        RejectPaymentRequest rejectRequest = new RejectPaymentRequest();
        rejectRequest.setReason("Incorrect beneficiary details");

        paymentService.rejectPayment(paymentId, "C1", rejectRequest);

        PaymentResponse rejectedPayment =
                paymentService.getPaymentById(paymentId);

        assertEquals(PaymentStatus.REJECTED, rejectedPayment.getStatus());
    }

    @Test
    void makerCannotExceedDailySubmissionLimit() {

        CreatePaymentRequest firstRequest = new CreatePaymentRequest();

        firstRequest.setBeneficiaryName("Beneficiary One");
        firstRequest.setBeneficiaryId("TEST010");
        firstRequest.setAmount(new BigDecimal("3000000.00"));
        firstRequest.setPurpose("First daily payment");

        Payment firstPayment =
                paymentService.createPayment("M1", firstRequest);

        paymentService.submitPayment(firstPayment.getId(), "M1");


        CreatePaymentRequest secondRequest = new CreatePaymentRequest();

        secondRequest.setBeneficiaryName("Beneficiary Two");
        secondRequest.setBeneficiaryId("TEST011");
        secondRequest.setAmount(new BigDecimal("2500000.00"));
        secondRequest.setPurpose("Second daily payment");

        Payment secondPayment =
                paymentService.createPayment("M1", secondRequest);

        assertThrows(
                BusinessException.class,
                () -> paymentService.submitPayment(secondPayment.getId(), "M1")
        );
    }

    @Test
    void rejectedPaymentDoesNotCountTowardDailyLimit() {

        CreatePaymentRequest firstRequest = new CreatePaymentRequest();
        firstRequest.setBeneficiaryName("Beneficiary One");
        firstRequest.setBeneficiaryId("TEST020");
        firstRequest.setAmount(new BigDecimal("3000000.00"));
        firstRequest.setPurpose("First payment");

        Payment firstPayment =
                paymentService.createPayment("M1", firstRequest);

        paymentService.submitPayment(firstPayment.getId(), "M1");

        RejectPaymentRequest rejectRequest = new RejectPaymentRequest();
        rejectRequest.setReason("Incorrect beneficiary details");

        paymentService.rejectPayment(
                firstPayment.getId(), "C1", rejectRequest
        );

        CreatePaymentRequest secondRequest = new CreatePaymentRequest();
        secondRequest.setBeneficiaryName("Beneficiary Two");
        secondRequest.setBeneficiaryId("TEST021");
        secondRequest.setAmount(new BigDecimal("3000000.00"));
        secondRequest.setPurpose("Second payment");

        Payment secondPayment =
                paymentService.createPayment("M1", secondRequest);

        PaymentResponse response =
                paymentService.submitPayment(secondPayment.getId(), "M1");

        assertEquals(PaymentStatus.SUBMITTED, response.getStatus());
    }


    @Test
    void makerCheckerCannotRejectOwnPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST060");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Self rejection test");

        Payment payment =
                paymentService.createPayment("MC1", request);

        paymentService.submitPayment(payment.getId(), "MC1");

        RejectPaymentRequest rejectRequest = new RejectPaymentRequest();
        rejectRequest.setReason("Testing self rejection");

        assertThrows(
                BusinessException.class,
                () -> paymentService.rejectPayment(
                        payment.getId(), "MC1", rejectRequest
                )
        );
    }

    @Test
    void makerCannotSubmitAnotherMakersPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST080");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Unauthorized submission test");

        Payment payment =
                paymentService.createPayment("M1", request);

        assertThrows(
                BusinessException.class,
                () -> paymentService.submitPayment(
                        payment.getId(), "M2"
                )
        );
    }

    @Test
    void makerWithoutCheckerRoleCannotApprovePayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST090");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Role validation test");

        Payment payment =
                paymentService.createPayment("M1", request);

        paymentService.submitPayment(payment.getId(), "M1");

        assertThrows(
                BusinessException.class,
                () -> paymentService.approvePayment(
                        payment.getId(), "M2"
                )
        );
    }

    @Test
    void adminCannotApprovePayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST100");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Admin approval validation");

        Payment payment =
                paymentService.createPayment("M1", request);

        paymentService.submitPayment(payment.getId(), "M1");

        assertThrows(
                BusinessException.class,
                () -> paymentService.approvePayment(
                        payment.getId(), "A1"
                )
        );
    }
    @Test
    void checkerCannotRejectPaymentWithoutReason() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST110");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Rejection reason validation");

        Payment payment =
                paymentService.createPayment("M1", request);

        paymentService.submitPayment(payment.getId(), "M1");

        RejectPaymentRequest rejectRequest = new RejectPaymentRequest();
        rejectRequest.setReason(" ");

        assertThrows(
                BusinessException.class,
                () -> paymentService.rejectPayment(
                        payment.getId(), "C1", rejectRequest
                )
        );
    }

    @Test
    void checkerCannotApproveRejectedPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST120");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Rejected payment validation");

        Payment payment =
                paymentService.createPayment("M1", request);

        paymentService.submitPayment(payment.getId(), "M1");

        RejectPaymentRequest rejectRequest = new RejectPaymentRequest();
        rejectRequest.setReason("Invalid beneficiary details");

        paymentService.rejectPayment(
                payment.getId(), "C1", rejectRequest
        );

        assertThrows(
                BusinessException.class,
                () -> paymentService.approvePayment(
                        payment.getId(), "C2"
                )
        );
    }
    @Test
    void makerCannotSubmitPaymentTwice() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBeneficiaryName("Test Beneficiary");
        request.setBeneficiaryId("TEST160");
        request.setAmount(new BigDecimal("500000.00"));
        request.setPurpose("Duplicate submission test");

        Payment payment =
                paymentService.createPayment("M1", request);

        paymentService.submitPayment(payment.getId(), "M1");

        assertThrows(
                BusinessException.class,
                () -> paymentService.submitPayment(
                        payment.getId(), "M1"
                )
        );
    }
}