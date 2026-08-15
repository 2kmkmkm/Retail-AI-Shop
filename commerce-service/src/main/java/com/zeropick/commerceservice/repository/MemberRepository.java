package com.zeropick.commerceservice.repository;

import com.zeropick.commerceservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
}
