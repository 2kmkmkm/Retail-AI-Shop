package com.zeropick.recommendationservice.repository;

import com.zeropick.recommendationservice.domain.RecoResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoResultRepository extends JpaRepository<RecoResult, Long> {
    List<RecoResult> findByMemberIdOrderByRankNoAsc(Long memberId);
    void deleteByMemberId(Long memberId);
}