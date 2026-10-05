package com.marketplace.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByResolvedFalseOrderByCreatedAtDesc();

    List<Report> findByProductIdAndResolvedFalse(Long productId);

    boolean existsByReporterIdAndProductIdAndResolvedFalse(Long reporterId, Long productId);

    boolean existsByReporterIdAndReviewIdAndResolvedFalse(Long reporterId, Long reviewId);
}
