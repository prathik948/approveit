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

}