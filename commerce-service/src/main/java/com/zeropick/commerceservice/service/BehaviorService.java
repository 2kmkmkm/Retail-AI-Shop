package com.zeropick.commerceservice.service;

import com.zeropick.commerceservice.dto.BehaviorCreateRequest;
import com.zeropick.commerceservice.event.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final ApplicationEventPublisher eventPublisher;

    public void create(BehaviorCreateRequest request) {
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        eventPublisher.publishEvent(new ProductViewedEvent(
                request.memberId(), request.productId(), request.category(), occurredAt
        ));
    }
}
