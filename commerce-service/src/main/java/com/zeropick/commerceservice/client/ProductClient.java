package com.zeropick.commerceservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/product-service/products/{id}")
    ProductSummary getProduct(@PathVariable("id") Long id);

    record ProductSummary(Long id, String name, Integer price, String category) {
    }
}
