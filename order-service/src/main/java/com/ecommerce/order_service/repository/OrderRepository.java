package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
