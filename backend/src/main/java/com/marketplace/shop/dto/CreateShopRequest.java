package com.marketplace.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(
        @NotBlank String name,
        String description
) {}
