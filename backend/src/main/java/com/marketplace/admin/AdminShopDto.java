package com.marketplace.admin;

import com.marketplace.shop.Shop;

import java.time.Instant;

public record AdminShopDto(Long id, Long ownerId, String name, boolean verified, Instant createdAt) {
    public static AdminShopDto from(Shop s) {
        return new AdminShopDto(s.getId(), s.getOwnerId(), s.getName(), s.isVerified(), s.getCreatedAt());
    }
}
