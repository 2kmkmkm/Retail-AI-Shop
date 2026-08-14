package com.zeropick.recommendationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "behavior_log", indexes = {
        @Index(name = "idx_behavior_member", columnList = "member_id, occurred_at"),
        @Index(name = "idx_behavior_type", columnList = "event_type, occurred_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "category", length = 30, nullable = false)
    private String category;

    @Column(name = "event_type", length = 20, nullable = false)
    private String eventType;

    private Integer qty;

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "order_no", length = 20)
    private String orderNo;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}