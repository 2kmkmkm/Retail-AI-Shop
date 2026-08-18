package com.zeropick.commerceservice.client;

import com.zeropick.commerceservice.exception.StockDeductionFailedException;
import com.zeropick.commerceservice.exception.StockRestoreFailedException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductClient productClient;

    public String deduct(Long productId, int qty) {
        try {
            ProductClient.ProductSummary product = productClient.deductStock(
                    productId, new ProductClient.StockRequest(qty)
            );
            if (product == null || product.id() == null) {
                throw new StockDeductionFailedException(productId);
            }
            return product.category() == null || product.category().isBlank()
                    ? "UNCATEGORIZED"
                    : product.category();
        } catch (FeignException exception) {
            throw new StockDeductionFailedException(productId, exception);
        }
    }

    public void restore(Long productId, int qty) {
        try {
            productClient.restoreStock(productId, new ProductClient.StockRequest(qty));
        } catch (FeignException exception) {
            throw new StockRestoreFailedException(productId, exception);
        }
    }
}
