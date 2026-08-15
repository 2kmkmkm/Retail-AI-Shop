package com.zeropick.commerceservice.controller;

import com.zeropick.commerceservice.dto.MemberCreateRequest;
import com.zeropick.commerceservice.dto.MemberResponse;
import com.zeropick.commerceservice.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commerce-service/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberCreateRequest request) {
        return memberService.create(request);
    }
}
