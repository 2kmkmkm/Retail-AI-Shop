package com.zeropick.commerceservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart",
                columnNames = {"member_id", "product_id"}
        )
)
@Check(constraints = "qty > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Builder
    public CartItem(Member member, Long productId, Integer qty) {
        this.member = member;
        this.productId = productId;
        this.qty = qty;
    }

    public void changeQuantity(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.qty = qty;
    }

    @PrePersist
    private void prePersist() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}
