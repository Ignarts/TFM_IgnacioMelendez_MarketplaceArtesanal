package com.marketplace.admin;

import com.marketplace.product.Product;

public record AdminProductDto(Long id, String title, Long shopId, String shopName, boolean hidden) {
    public static AdminProductDto from(Product p) {
        return new AdminProductDto(p.getId(), p.getTitle(), p.getShop().getId(), p.getShop().getName(), p.isHidden());
    }
}
