package com.marketplace.product;

import com.marketplace.category.Category;
import com.marketplace.shop.Shop;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @PositiveOrZero
    @Column(nullable = false)
    private int stock;

    // ponytail: image URLs only, no file upload until the project needs it
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "url", length = 1000) // image URLs can be long (e.g. CDN thumbnails)
    private List<String> images = new ArrayList<>();

    // Set by an admin when moderating a reported product: it disappears from the public catalog
    // but stays in the database so past orders and reviews keep pointing at it.
    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Product() {}

    public Long getId() { return id; }

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public Instant getCreatedAt() { return createdAt; }
}
