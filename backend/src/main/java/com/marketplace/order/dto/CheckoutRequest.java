package com.marketplace.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty @Valid List<Line> items
) {
    public record Line(
            @NotNull Long productId,
            @Positive int quantity
    ) {}
}
