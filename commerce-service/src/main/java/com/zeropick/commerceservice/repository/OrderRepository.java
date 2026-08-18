package com.zeropick.commerceservice.repository;

import com.zeropick.commerceservice.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CommerceOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
