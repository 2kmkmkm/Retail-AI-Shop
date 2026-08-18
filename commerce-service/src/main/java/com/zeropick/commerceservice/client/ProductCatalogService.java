package com.zeropick.commerceservice.client;

import com.zeropick.commerceservice.exception.ProductNotFoundException;
import com.zeropick.commerceservice.exception.ProductServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductClient productClient;

    public String getCategory(Long productId) {
        try {
            ProductClient.ProductSummary product = productClient.getProduct(productId);
            if (product == null || product.id() == null) {
                throw new ProductNotFoundException(productId);
            }
            return product.category() == null || product.category().isBlank()
                    ? "UNCATEGORIZED"
                    : product.category();
        } catch (FeignException.NotFound exception) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException exception) {
            throw new ProductServiceUnavailableException(exception);
        }
    }
}
