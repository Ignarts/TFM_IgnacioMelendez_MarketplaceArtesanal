package com.marketplace.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByBuyerId(Long buyerId);

    long countByProduct_ShopId(Long shopId);

    boolean existsByProductIdAndBuyerId(Long productId, Long buyerId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT AVG(r.rating) FROM Review r WHERE r.product.shop.id = :shopId")
    java.math.BigDecimal avgRatingByShopId(@org.springframework.data.repository.query.Param("shopId") Long shopId);

    /** Suspicious pattern: same buyer leaving many reviews in the same shop recently. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(r) FROM Review r WHERE r.product.shop.id = :shopId " +
            "AND r.buyerId = :buyerId AND r.createdAt >= :since")
    long countRecentReviewsByBuyerInShop(
            @org.springframework.data.repository.query.Param("shopId") Long shopId,
            @org.springframework.data.repository.query.Param("buyerId") Long buyerId,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
