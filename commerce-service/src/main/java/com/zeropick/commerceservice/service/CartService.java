package com.zeropick.commerceservice.service;

import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.dto.CartAddRequest;
import com.zeropick.commerceservice.dto.CartItemResponse;
import com.zeropick.commerceservice.dto.CartUpdateRequest;
import com.zeropick.commerceservice.entity.CartItem;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.event.CartAddedEvent;
import com.zeropick.commerceservice.exception.CartItemNotFoundException;
import com.zeropick.commerceservice.exception.MemberNotFoundException;
import com.zeropick.commerceservice.repository.CartItemRepository;
import com.zeropick.commerceservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ProductCatalogService productCatalogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CartItemResponse> findByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException(memberId);
        }
        return cartItemRepository.findAllByMemberIdOrderByAddedAtAsc(memberId).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse add(CartAddRequest request) {
        Member member = memberRepository.findByIdForUpdate(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(request.memberId()));
        String category = productCatalogService.getCategory(request.productId());

        CartItem item = cartItemRepository
                .findByMemberIdAndProductId(request.memberId(), request.productId())
                .map(existing -> {
                    existing.changeQuantity(existing.getQty() + request.qty());
                    return existing;
                })
                .orElseGet(() -> CartItem.builder()
                        .member(member)
                        .productId(request.productId())
                        .qty(request.qty())
                        .build());

        CartItem saved = cartItemRepository.save(item);
        eventPublisher.publishEvent(new CartAddedEvent(
                request.memberId(), request.productId(), category, request.qty(), Instant.now()
        ));
        return CartItemResponse.from(saved);
    }

    @Transactional
    public CartItemResponse update(Long cartItemId, CartUpdateRequest request) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
        item.changeQuantity(request.qty());
        return CartItemResponse.from(item);
    }

    @Transactional
    public void delete(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
        cartItemRepository.delete(item);
    }
}
