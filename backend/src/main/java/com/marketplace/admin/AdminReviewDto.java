package com.marketplace.admin;

import com.marketplace.review.Review;

import java.time.Instant;

public record AdminReviewDto(
        Long id,
        Long productId,
        String productTitle,
        String buyerName,
        int rating,
        String comment,
        Instant createdAt
) {
    public static AdminReviewDto from(Review r) {
        return new AdminReviewDto(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getTitle(),
                r.getBuyerName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
