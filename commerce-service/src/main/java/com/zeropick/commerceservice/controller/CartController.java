package com.zeropick.commerceservice.controller;

import com.zeropick.commerceservice.dto.CartAddRequest;
import com.zeropick.commerceservice.dto.CartItemResponse;
import com.zeropick.commerceservice.dto.CartUpdateRequest;
import com.zeropick.commerceservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/commerce-service/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{memberId}")
    public List<CartItemResponse> findByMemberId(@PathVariable Long memberId) {
        return cartService.findByMemberId(memberId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse add(@Valid @RequestBody CartAddRequest request) {
        return cartService.add(request);
    }

    @PutMapping("/{cartItemId}")
    public CartItemResponse update(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateRequest request
    ) {
        return cartService.update(cartItemId, request);
    }

    @DeleteMapping("/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
    }
}
