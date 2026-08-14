package com.zeropick.recommendationservice.repository;

import com.zeropick.recommendationservice.domain.RecoClick;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoClickRepository extends JpaRepository<RecoClick, Long> {
}