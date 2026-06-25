package com.marketplace.order.dto;

import com.marketplace.order.Order;
import com.marketplace.order.OrderItem;
import com.marketplace.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long buyerId,
        Long shopId,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt,
        List<Item> items
) {

    public record Item(Long productId, String title, BigDecimal price, int quantity) {
        static Item from(OrderItem oi) {
            return new Item(oi.getProduct().getId(), oi.getProduct().getTitle(), oi.getPrice(), oi.getQuantity());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerId(),
                order.getShopId(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().stream().map(Item::from).toList()
        );
    }
}
