package com.zeropick.recommendationservice.repository;

import com.zeropick.recommendationservice.domain.BehaviorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BehaviorLogRepository extends JpaRepository<BehaviorLog, Long> {
    List<BehaviorLog> findByMemberIdOrderByOccurredAtDesc(Long memberId);
}