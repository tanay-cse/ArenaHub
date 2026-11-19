package com.arenahub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByIsActiveTrue();

    List<PricingRule> findAllByOrderByIdAsc();
}
