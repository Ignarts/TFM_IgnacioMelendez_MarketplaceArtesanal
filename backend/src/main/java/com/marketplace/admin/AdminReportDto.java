package com.marketplace.admin;

import com.marketplace.product.Product;
import com.marketplace.report.Report;
import com.marketplace.report.ReportTargetType;
import com.marketplace.review.Review;

import java.time.Instant;

/** A moderation queue entry with enough context about the reported content to decide. */
public record AdminReportDto(
        Long id,
        ReportTargetType type,
        Long targetId,
        Long productId,
        String productTitle,
        String reviewAuthor,
        Integer reviewRating,
        String reviewComment,
        String reason,
        String reporterName,
        Instant createdAt
) {
    public static AdminReportDto from(Report r) {
        if (r.getTargetType() == ReportTargetType.REVIEW) {
            Review review = r.getReview();
            return new AdminReportDto(r.getId(), r.getTargetType(), review.getId(),
                    review.getProduct().getId(), review.getProduct().getTitle(),
                    review.getBuyerName(), review.getRating(), review.getComment(),
                    r.getReason(), r.getReporterName(), r.getCreatedAt());
        }
        Product product = r.getProduct();
        return new AdminReportDto(r.getId(), r.getTargetType(), product.getId(),
                product.getId(), product.getTitle(), null, null, null,
                r.getReason(), r.getReporterName(), r.getCreatedAt());
    }
}
