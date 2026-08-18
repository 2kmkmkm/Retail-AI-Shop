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
        return getProduct(productId).category();
    }

    public CatalogProduct getProduct(Long productId) {
        try {
            ProductClient.ProductSummary product = productClient.getProduct(productId);
            if (product == null || product.id() == null) {
                throw new ProductNotFoundException(productId);
            }
            if (product.name() == null || product.name().isBlank()
                    || product.price() == null || product.price() < 0) {
                throw new ProductServiceUnavailableException(
                        new IllegalStateException("상품 응답에 이름 또는 가격이 없습니다.")
                );
            }
            String category = product.category() == null || product.category().isBlank()
                    ? "UNCATEGORIZED"
                    : product.category();
            return new CatalogProduct(product.id(), product.name(), product.price().longValue(), category);
        } catch (FeignException.NotFound exception) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException exception) {
            throw new ProductServiceUnavailableException(exception);
        }
    }

    public record CatalogProduct(Long id, String name, Long price, String category) {
    }
}
