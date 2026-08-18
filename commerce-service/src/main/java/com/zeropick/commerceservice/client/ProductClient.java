package com.zeropick.commerceservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/product-service/products/{id}")
    ProductSummary getProduct(@PathVariable("id") Long id);

    @PutMapping("/product-service/products/{id}/stock/deduct")
    ProductSummary deductStock(@PathVariable("id") Long id, @RequestBody StockRequest request);

    @PutMapping("/product-service/products/{id}/stock/restore")
    ProductSummary restoreStock(@PathVariable("id") Long id, @RequestBody StockRequest request);

    record ProductSummary(Long id, String name, Integer price, String category) {
    }

    record StockRequest(Integer qty) {
    }
}
