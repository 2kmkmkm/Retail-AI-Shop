package com.zeropick.productservice.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "product_allergen")
public class ProductAllergen {

    @EmbeddedId
    private ProductAllergenId id;

    protected ProductAllergen() {
        // JPA 기본 생성자
    }

    public ProductAllergen(Long productId, String allergen) {
        this.id = new ProductAllergenId(productId, allergen);
    }

    public ProductAllergenId getId() {
        return id;
    }
}
