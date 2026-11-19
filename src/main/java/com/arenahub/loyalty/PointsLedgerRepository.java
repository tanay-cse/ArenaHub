package com.arenahub.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {

    List<PointsLedger> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(p.pointsChange), 0L) FROM PointsLedger p WHERE p.user.id = :userId")
    Long calculateBalanceByUserId(@Param("userId") Long userId);
}
