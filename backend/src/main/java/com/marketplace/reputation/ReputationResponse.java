package com.marketplace.reputation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record ReputationResponse(
        Long shopId,
        int score,
        int nSales,
        BigDecimal avgRating,
        int ageDays,
        BigDecimal incidentRate,
        Set<Badge> badges,
        Instant updatedAt
) {
    public static ReputationResponse from(Reputation r) {
        return new ReputationResponse(
                r.getShopId(),
                r.getScore(),
                r.getNSales(),
                r.getAvgRating(),
                r.getAgeDays(),
                r.getIncidentRate(),
                r.getBadges(),
                r.getUpdatedAt()
        );
    }
}
