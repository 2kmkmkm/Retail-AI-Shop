package com.zeropick.productservice.repository;

import com.zeropick.productservice.domain.Product;
import com.zeropick.productservice.domain.ProductSweetener;
import com.zeropick.productservice.domain.ProductAllergen;
import com.zeropick.productservice.domain.Sweetener;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(String category) {
        if (category == null || category.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Product> sugarLessThanOrEqual(BigDecimal sugarMax) {
        if (sugarMax == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("sugarG"), sugarMax);
    }

    public static Specification<Product> kcalBetween(BigDecimal kcalMin, BigDecimal kcalMax) {
        if (kcalMin == null && kcalMax == null) return null;
        return (root, query, cb) -> {
            if (kcalMin != null && kcalMax != null) {
                return cb.between(root.get("kcal"), kcalMin, kcalMax);
            } else if (kcalMin != null) {
                return cb.greaterThanOrEqualTo(root.get("kcal"), kcalMin);
            } else {
                return cb.lessThanOrEqualTo(root.get("kcal"), kcalMax);
            }
        };
    }

    public static Specification<Product> nameOrBrandContains(String q) {
        if (q == null || q.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + q.trim() + "%";
            return cb.or(
                    cb.like(root.get("name"), pattern),
                    cb.like(root.get("brand"), pattern)
            );
        };
    }

    public static Specification<Product> excludesSweeteners(List<String> excludedNames) {
        if (excludedNames == null || excludedNames.isEmpty()) return null;
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProductSweetener> psRoot = subquery.from(ProductSweetener.class);

            Subquery<Long> sweetenerIdsSub = query.subquery(Long.class);
            Root<Sweetener> swRoot = sweetenerIdsSub.from(Sweetener.class);
            sweetenerIdsSub.select(swRoot.get("id")).where(swRoot.get("name").in(excludedNames));

            subquery.select(psRoot.get("id").get("productId"))
                    .where(
                            cb.equal(psRoot.get("id").get("productId"), root.get("id")),
                            psRoot.get("id").get("sweetenerId").in(sweetenerIdsSub)
                    );

            return cb.not(cb.exists(subquery));
        };
    }

    public static Specification<Product> excludesAllergens(List<String> excludedAllergens) {
        if (excludedAllergens == null || excludedAllergens.isEmpty()) return null;
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProductAllergen> paRoot = subquery.from(ProductAllergen.class);
            subquery.select(paRoot.get("id").get("productId"))
                    .where(
                            cb.equal(paRoot.get("id").get("productId"), root.get("id")),
                            paRoot.get("id").get("allergen").in(excludedAllergens)
                    );
            return cb.not(cb.exists(subquery));
        };
    }
}
