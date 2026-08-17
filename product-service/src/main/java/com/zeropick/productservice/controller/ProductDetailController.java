package com.zeropick.productservice.controller;

import com.zeropick.productservice.dto.ProductDetailResponse;
import com.zeropick.productservice.exception.ProductNotFoundException;
import com.zeropick.productservice.service.ProductDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product-service/products")
public class ProductDetailController {

    private final ProductDetailService productDetailService;

    public ProductDetailController(ProductDetailService productDetailService) {
        this.productDetailService = productDetailService;
    }

    @GetMapping("/compare")
    public List<ProductDetailResponse> compare(@RequestParam String ids) {
        return productDetailService.compare(ids);
    }

    @GetMapping("/{id:[0-9]+}")
    public ProductDetailResponse getDetail(@PathVariable Long id) {
        return productDetailService.getDetail(id);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ProductNotFoundException e) {
        return e.getMessage();
    }
}
