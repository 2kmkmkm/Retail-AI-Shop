package com.zeropick.commerceservice.controller;

import com.zeropick.commerceservice.dto.BehaviorCreateRequest;
import com.zeropick.commerceservice.service.BehaviorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commerce-service/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorService behaviorService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void create(@Valid @RequestBody BehaviorCreateRequest request) {
        behaviorService.create(request);
    }
}
