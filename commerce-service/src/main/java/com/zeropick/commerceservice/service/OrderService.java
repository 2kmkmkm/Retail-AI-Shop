package com.zeropick.commerceservice.service;

import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.dto.OrderCreateRequest;
import com.zeropick.commerceservice.dto.OrderResponse;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.entity.Order;
import com.zeropick.commerceservice.entity.OrderItem;
import com.zeropick.commerceservice.exception.MemberNotFoundException;
import com.zeropick.commerceservice.repository.MemberRepository;
import com.zeropick.commerceservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final long ORDER_NUMBER_OFFSET = 1000L;

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductCatalogService productCatalogService;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(request.memberId()));

        Order order = Order.builder()
                .orderNo(temporaryOrderNo())
                .member(member)
                .totalPrice(0L)
                .build();

        long totalPrice = 0L;
        for (OrderCreateRequest.Item requestedItem : request.items()) {
            ProductCatalogService.CatalogProduct product =
                    productCatalogService.getProduct(requestedItem.productId());
            totalPrice = Math.addExact(
                    totalPrice,
                    Math.multiplyExact(product.price(), requestedItem.qty().longValue())
            );
            order.addItem(OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .qty(requestedItem.qty())
                    .unitPrice(product.price())
                    .build());
        }

        order.changeTotalPrice(totalPrice);
        Order saved = orderRepository.saveAndFlush(order);
        saved.assignOrderNo("ZP" + (ORDER_NUMBER_OFFSET + saved.getId()));
        orderRepository.flush();
        return OrderResponse.from(saved);
    }

    private String temporaryOrderNo() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
