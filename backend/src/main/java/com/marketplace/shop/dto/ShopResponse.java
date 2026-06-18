package com.marketplace.shop.dto;

import com.marketplace.shop.Shop;

public record ShopResponse(Long id, Long ownerId, String name, String description, boolean verified) {

    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getOwnerId(),
                shop.getName(),
                shop.getDescription(),
                shop.isVerified()
        );
    }
}
