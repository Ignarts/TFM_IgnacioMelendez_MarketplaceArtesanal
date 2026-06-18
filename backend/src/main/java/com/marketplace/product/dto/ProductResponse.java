package com.marketplace.product.dto;

import com.marketplace.product.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        Long shopId,
        String shopName,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        int stock,
        List<String> images
) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getShop().getId(),
                p.getShop().getName(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                List.copyOf(p.getImages())
        );
    }
}
