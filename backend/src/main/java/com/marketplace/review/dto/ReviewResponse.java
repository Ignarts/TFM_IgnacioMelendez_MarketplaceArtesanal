package com.marketplace.review.dto;

import com.marketplace.review.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        String buyerName,
        int rating,
        String comment,
        Instant createdAt
) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getBuyerName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
