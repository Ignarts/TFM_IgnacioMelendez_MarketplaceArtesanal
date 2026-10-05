package com.marketplace.review;

import com.marketplace.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "buyer_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    // Denormalized so listing reviews doesn't need a join to users.
    @Column(nullable = false)
    private String buyerName;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Public answer from the shop owner (one per review, editable).
    @Column(columnDefinition = "TEXT")
    private String sellerReply;

    private Instant sellerReplyAt;

    public Review() {}

    public Review(Product product, Long buyerId, String buyerName, int rating, String comment) {
        this.product = product;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }

    public String getSellerReply() { return sellerReply; }
    public void setSellerReply(String sellerReply) { this.sellerReply = sellerReply; }

    public Instant getSellerReplyAt() { return sellerReplyAt; }
    public void setSellerReplyAt(Instant sellerReplyAt) { this.sellerReplyAt = sellerReplyAt; }
}
