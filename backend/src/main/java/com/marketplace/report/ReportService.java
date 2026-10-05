package com.marketplace.report;

import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.report.dto.ReportRequest;
import com.marketplace.review.Review;
import com.marketplace.review.ReviewRepository;
import com.marketplace.user.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    /** Reporter name shown for reports raised automatically by the anti-fraud check. */
    public static final String SYSTEM_REPORTER = "Sistema antifraude";

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public ReportService(ReportRepository reportRepository, ProductRepository productRepository,
                         ReviewRepository reviewRepository) {
        this.reportRepository = reportRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    /** Any authenticated user can report content; one open report per user and target. */
    @Transactional
    public Report create(User reporter, ReportRequest request) {
        String reason = request.reason().trim();
        return switch (request.type()) {
            case PRODUCT -> {
                Product product = productRepository.findById(request.targetId())
                        .filter(p -> !p.isHidden())
                        .orElseThrow(() -> new NotFoundException("Product not found"));
                if (reportRepository.existsByReporterIdAndProductIdAndResolvedFalse(reporter.getId(), product.getId())) {
                    throw new ConflictException("You already reported this product");
                }
                yield reportRepository.save(Report.ofProduct(product, reporter.getId(), reporter.getName(), reason));
            }
            case REVIEW -> {
                Review review = reviewRepository.findById(request.targetId())
                        .orElseThrow(() -> new NotFoundException("Review not found"));
                if (reportRepository.existsByReporterIdAndReviewIdAndResolvedFalse(reporter.getId(), review.getId())) {
                    throw new ConflictException("You already reported this review");
                }
                yield reportRepository.save(Report.ofReview(review, reporter.getId(), reporter.getName(), reason));
            }
        };
    }

    /** Queues a review for moderation on behalf of the anti-fraud check (no human reporter). */
    @Transactional
    public Report flagReview(Review review, String reason) {
        return reportRepository.save(Report.ofReview(review, null, SYSTEM_REPORTER, reason));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Report> listOpen() {
        return reportRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    /** Closes a report without acting on the content (the complaint was unfounded). */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Report dismiss(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));
        report.setResolved(true);
        return reportRepository.save(report);
    }

    /** Closes every open report of a product once the admin has acted on it. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resolveForProduct(Long productId) {
        reportRepository.findByProductIdAndResolvedFalse(productId)
                .forEach(report -> report.setResolved(true));
    }
}
