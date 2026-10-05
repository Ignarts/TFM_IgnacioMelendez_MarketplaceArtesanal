package com.marketplace.report;

import com.marketplace.product.Product;
import com.marketplace.review.Review;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A user's complaint about a product or a review, queued for admin moderation.
 * Exactly one of {@code product} / {@code review} is set, depending on {@code targetType}.
 * Deleting the reported content deletes its reports too (ON DELETE CASCADE).
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    // Null when the report was raised by the anti-fraud check instead of a user.
    @Column(name = "reporter_id")
    private Long reporterId;

    // Denormalized so the moderation queue doesn't need a join to users.
    @Column(nullable = false)
    private String reporterName;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Report() {}

    public static Report ofProduct(Product product, Long reporterId, String reporterName, String reason) {
        Report report = new Report(ReportTargetType.PRODUCT, reporterId, reporterName, reason);
        report.product = product;
        return report;
    }

    public static Report ofReview(Review review, Long reporterId, String reporterName, String reason) {
        Report report = new Report(ReportTargetType.REVIEW, reporterId, reporterName, reason);
        report.review = review;
        return report;
    }

    private Report(ReportTargetType targetType, Long reporterId, String reporterName, String reason) {
        this.targetType = targetType;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reason = reason;
    }

    public Long getId() { return id; }

    public ReportTargetType getTargetType() { return targetType; }

    public Product getProduct() { return product; }

    public Review getReview() { return review; }

    public Long getReporterId() { return reporterId; }

    public String getReporterName() { return reporterName; }

    public String getReason() { return reason; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public Instant getCreatedAt() { return createdAt; }
}
