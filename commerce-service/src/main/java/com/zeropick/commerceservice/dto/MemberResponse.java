package com.zeropick.commerceservice.dto;

import com.zeropick.commerceservice.entity.Member;

public record MemberResponse(Long id, String email, String name) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName());
    }
}
