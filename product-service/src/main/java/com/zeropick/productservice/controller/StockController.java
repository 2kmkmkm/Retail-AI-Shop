package com.zeropick.productservice.controller;

import com.zeropick.productservice.domain.Product;
import com.zeropick.productservice.exception.OutOfStockException;
import com.zeropick.productservice.exception.ProductNotFoundException;
import com.zeropick.productservice.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-service/products/{id}/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PutMapping("/deduct")
    public Product deduct(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return stockService.deduct(id, request.qty());
    }

    @PutMapping("/restore")
    public Product restore(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return stockService.restore(id, request.qty());
    }

    @ExceptionHandler(OutOfStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOutOfStock(OutOfStockException e) {
        return e.getMessage();
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ProductNotFoundException e) {
        return e.getMessage();
    }

    public record StockRequest(@NotNull @Min(1) Integer qty) {
    }
}
