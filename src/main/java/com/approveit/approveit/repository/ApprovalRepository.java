
package com.approveit.approveit.repository;

import com.approveit.approveit.enums.ApprovalDecision;
import com.approveit.approveit.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    void deleteByPayment_Id(Long paymentId);

    boolean existsByPayment_IdAndChecker_UserId(
            Long paymentId, String checkerId);

    long countByPayment_IdAndDecision(
            Long paymentId, ApprovalDecision decision);
}

