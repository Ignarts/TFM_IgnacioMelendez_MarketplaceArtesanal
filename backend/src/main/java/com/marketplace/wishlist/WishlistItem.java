package com.marketplace.wishlist;

import com.marketplace.product.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/** A product a buyer saved as favourite. Deleting the product removes it from every wishlist. */
@Entity
@Table(name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WishlistItem() {}

    public WishlistItem(Long userId, Product product) {
        this.userId = userId;
        this.product = product;
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public Product getProduct() { return product; }

    public Instant getCreatedAt() { return createdAt; }
}
