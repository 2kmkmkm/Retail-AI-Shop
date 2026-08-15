package com.zeropick.commerceservice.dto;

import com.zeropick.commerceservice.entity.Member;

public record MemberLoginResponse(Long memberId, String name, String token, long expiresIn) {
    //멤버 엔티티에서 id와 name을 받아와 responseDTO생성
    public static MemberLoginResponse from(Member member, String token, long expiresIn) {
        return new MemberLoginResponse(member.getId(), member.getName(), token, expiresIn);
    }
}
