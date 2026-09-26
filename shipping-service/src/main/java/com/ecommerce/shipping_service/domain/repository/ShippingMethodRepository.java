package com.ecommerce.shipping_service.domain.repository;

import com.ecommerce.shipping_service.domain.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {
    boolean existsByIdAndActiveTrue(UUID id);
}
