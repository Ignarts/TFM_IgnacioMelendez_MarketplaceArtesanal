package com.marketplace.reputation;

import com.marketplace.order.OrderRepository;
import com.marketplace.order.OrderStatus;
import com.marketplace.review.ReviewRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Computes and materialises the reputation aggregate for each shop.
 *
 * Formula (weights sum to 1.0):
 *   score = 0.40 · avgRatingNorm        (perceived quality)
 *         + 0.30 · salesVolumeNorm       (track record)
 *         + 0.20 · ageNorm               (platform seniority)
 *         - 0.10 · incidentRate          (incident penalty)
 *   Result scaled to 0–100 and clamped to [0, 100].
 *
 * Normalisation:
 *   avgRatingNorm     = avgRating / 5
 *   salesVolumeNorm   = min(nSales / 100, 1)   (saturates at 100 sales)
 *   ageNorm           = min(ageDays / 365, 1)   (saturates at 1 year)
 *   incidentRate      = cancelledOrders / totalOrders  ∈ [0,1]
 */
@Service
public class ReputationService {

    private static final double W_RATING   = 0.40;
    private static final double W_SALES    = 0.30;
    private static final double W_AGE      = 0.20;
    private static final double W_INCIDENT = 0.10;

    private static final int SALES_SATURATION = 100;
    private static final int AGE_SATURATION_DAYS = 365;
    private static final int FEATURED_THRESHOLD = 85;
    private static final int FAST_RESPONSE_MIN_SALES = 10;
    private static final double FAST_RESPONSE_MAX_INCIDENT_RATE = 0.05;

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ReputationRepository reputationRepository;

    public ReputationService(ShopRepository shopRepository,
                             OrderRepository orderRepository,
                             ReviewRepository reviewRepository,
                             ReputationRepository reputationRepository) {
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.reputationRepository = reputationRepository;
    }

    /** Recalculates reputation for all shops every hour. */
    @Scheduled(fixedRateString = "${reputation.recalculate-interval-ms:3600000}")
    @Transactional
    public void recalculateAll() {
        List<Shop> shops = shopRepository.findAll();
        for (Shop shop : shops) {
            recalculate(shop);
        }
    }

    /** On-demand recalculation for a single shop (triggered after key events). */
    @Transactional
    public Reputation recalculateForShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));
        return recalculate(shop);
    }

    @Transactional(readOnly = true)
    public Optional<Reputation> findByShopId(Long shopId) {
        return reputationRepository.findByShopId(shopId);
    }

    // ── internal ─────────────────────────────────────────────────────────────

    Reputation recalculate(Shop shop) {
        Long shopId = shop.getId();

        long nSales = orderRepository.countByShopIdAndStatus(shopId, OrderStatus.DELIVERED);
        long totalOrders = orderRepository.countByShopId(shopId);

        BigDecimal avgRatingRaw = reviewRepository.avgRatingByShopId(shopId);
        BigDecimal avgRating = avgRatingRaw != null
                ? avgRatingRaw.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long ageDays = ChronoUnit.DAYS.between(shop.getCreatedAt(), Instant.now());

        BigDecimal incidentRate = totalOrders > 0
                ? BigDecimal.valueOf(totalOrders - nSales)
                        .divide(BigDecimal.valueOf(totalOrders), 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int score = computeScore(nSales, avgRating, ageDays, incidentRate);
        Set<Badge> badges = computeBadges(shop, score, nSales, incidentRate);

        Reputation rep = reputationRepository.findByShopId(shopId)
                .orElse(new Reputation(shopId));
        rep.setScore(score);
        rep.setNSales((int) nSales);
        rep.setAvgRating(avgRating);
        rep.setAgeDays((int) ageDays);
        rep.setIncidentRate(incidentRate);
        rep.setBadges(badges);
        rep.setUpdatedAt(Instant.now());

        return reputationRepository.save(rep);
    }

    int computeScore(long nSales, BigDecimal avgRating, long ageDays, BigDecimal incidentRate) {
        double ratingNorm   = avgRating.doubleValue() / 5.0;
        double salesNorm    = Math.min((double) nSales / SALES_SATURATION, 1.0);
        double ageNorm      = Math.min((double) ageDays / AGE_SATURATION_DAYS, 1.0);
        double incident     = incidentRate.doubleValue();

        double raw = W_RATING   * ratingNorm
                   + W_SALES    * salesNorm
                   + W_AGE      * ageNorm
                   - W_INCIDENT * incident;

        return (int) Math.round(Math.max(0, Math.min(100, raw * 100)));
    }

    private Set<Badge> computeBadges(Shop shop, int score, long nSales, BigDecimal incidentRate) {
        Set<Badge> badges = EnumSet.noneOf(Badge.class);

        if (shop.isVerified()) {
            badges.add(Badge.VERIFIED);
        }
        if (score > FEATURED_THRESHOLD) {
            badges.add(Badge.FEATURED);
        }
        if (nSales >= OVER_100_SALES_THRESHOLD) {
            badges.add(Badge.OVER_100_SALES);
        }
        // Fast response: heuristic — active shop with low incident rate
        if (nSales >= FAST_RESPONSE_MIN_SALES
                && incidentRate.doubleValue() < FAST_RESPONSE_MAX_INCIDENT_RATE) {
            badges.add(Badge.FAST_RESPONSE);
        }

        return badges;
    }

    private static final int OVER_100_SALES_THRESHOLD = 100;
}
