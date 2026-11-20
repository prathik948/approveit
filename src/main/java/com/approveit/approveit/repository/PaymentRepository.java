
package com.approveit.approveit.repository;

import com.approveit.approveit.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Fetch Payment and Maker together
    @Override
    @EntityGraph(attributePaths = {"maker"})
    List<Payment> findAll();

    // Fetch one Payment and its Maker together
    @Override
    @EntityGraph(attributePaths = {"maker"})
    Optional<Payment> findById(Long id);

    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.maker.userId = :makerId
    AND p.status NOT IN (
        com.approveit.approveit.enums.PaymentStatus.DRAFT,
        com.approveit.approveit.enums.PaymentStatus.REJECTED
    )
    AND p.submittedAt >= :startOfDay
    AND p.submittedAt < :startOfNextDay
""")
    BigDecimal getDailySubmittedAmount(
            @Param("makerId") String makerId,
            @Param("startOfDay") Instant startOfDay,
            @Param("startOfNextDay") Instant startOfNextDay
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") Long paymentId);
}

