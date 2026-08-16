package com.zeropick.productservice.service;

import com.zeropick.productservice.domain.Product;
import com.zeropick.productservice.exception.OutOfStockException;
import com.zeropick.productservice.exception.ProductNotFoundException;
import com.zeropick.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product deduct(Long productId, int qty) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        int updated = productRepository.deductStock(productId, qty);
        if (updated == 0) {
            throw new OutOfStockException(productId);
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional
    public Product restore(Long productId, int qty) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.restoreStock(productId, qty);
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
