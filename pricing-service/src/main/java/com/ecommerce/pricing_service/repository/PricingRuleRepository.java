package com.ecommerce.pricing_service.repository;

import com.ecommerce.pricing_service.domain.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    @Query("SELECT p FROM PricingRule p WHERE :now BETWEEN p.startDate AND p.endDate")
    List<PricingRule> findActiveRules(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM PricingRule p WHERE p.productId = :productId AND :now BETWEEN p.startDate AND p.endDate")
    List<PricingRule> findActiveRuleByProductId(@Param("productId") UUID productId, @Param("now") LocalDateTime now);
}
