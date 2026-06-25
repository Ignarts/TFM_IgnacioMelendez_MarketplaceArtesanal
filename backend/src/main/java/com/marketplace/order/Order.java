package com.marketplace.order;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as ids to keep ownership checks simple, like Shop.ownerId.
    @Column(nullable = false)
    private Long buyerId;

    // One order per shop (checkout splits a multi-shop cart). Lets the seller own the
    // whole order and keeps per-shop sales available for the M3 reputation aggregate.
    @Column(nullable = false)
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Order() {}

    public Order(Long buyerId, Long shopId) {
        this.buyerId = buyerId;
        this.shopId = shopId;
    }

    /** Adds a line and keeps the order total in sync. */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    public Long getId() { return id; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public Instant getCreatedAt() { return createdAt; }
}
