package com.zeropick.commerceservice.repository;

import com.zeropick.commerceservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
