package com.marketplace.review.dto;

import com.marketplace.review.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        String productTitle,
        String buyerName,
        int rating,
        String comment,
        Instant createdAt,
        String sellerReply,
        Instant sellerReplyAt
) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getTitle(),
                r.getBuyerName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.getSellerReply(),
                r.getSellerReplyAt()
        );
    }
}
