package com.marketplace.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank String title,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero int stock,
        @NotNull Long categoryId,
        List<String> images
) {}
