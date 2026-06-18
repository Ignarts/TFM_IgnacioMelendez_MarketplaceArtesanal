package com.marketplace.product;

import com.marketplace.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Explorador público de productos")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con filtros opcionales")
    public List<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.search(q, categoryId, minPrice, maxPrice).stream()
                .map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un producto")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(productService.getById(id));
    }
}
