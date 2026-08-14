package com.zeropick.productservice.repository;

import com.zeropick.productservice.domain.Sweetener;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SweetenerRepository extends JpaRepository<Sweetener, Long> {
}
