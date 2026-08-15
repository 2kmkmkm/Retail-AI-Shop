package com.zeropick.commerceservice.service;

import com.zeropick.commerceservice.dto.MemberCreateRequest;
import com.zeropick.commerceservice.dto.MemberResponse;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.exception.DuplicateEmailException;
import com.zeropick.commerceservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        Member member = Member.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .build();

        try {
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
