package com.zeropick.commerceservice.entity;

import com.zeropick.commerceservice.exception.InvalidOrderStatusException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
@Getter
@Entity(name = "CommerceOrder")
@Table(
        name = "orders",
        indexes = @Index(
                name = "idx_orders_member",
                columnList = "member_id, ordered_at"
        )
)
@Check(constraints = "total_price >= 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 20)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OrderStatus status;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    @Builder
    public Order(String orderNo, Member member, Long totalPrice) {
        this.orderNo = orderNo;
        this.member = member;
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }

    public void assignOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void changeTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void markPaid(String paymentMethod) {
        validatePayable();
        this.status = OrderStatus.PAID;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }

    public void validatePayable() {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(id, status);
        }
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public void validateCancellable() {
        if (status != OrderStatus.PENDING && status != OrderStatus.PAID) {
            throw new InvalidOrderStatusException(id, status);
        }
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        validateCancellable();
        this.status = OrderStatus.CANCELLED;
    }

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
    }
}
