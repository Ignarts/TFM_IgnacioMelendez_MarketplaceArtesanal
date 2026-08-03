package com.marketplace.reputation;

import com.marketplace.order.OrderRepository;
import com.marketplace.order.OrderStatus;
import com.marketplace.review.ReviewRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReputationServiceTest {

    @Mock private ShopRepository shopRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReputationRepository reputationRepository;

    @InjectMocks private ReputationService service;

    private Shop shop;

    @BeforeEach
    void setUp() {
        shop = new Shop(1L, "Test Shop", "desc");
        // Shop.getId() returns null (no JPA context) — service uses it as the key for
        // findByShopId; stubbed with any() so null is accepted.
        when(reputationRepository.findByShopId(any())).thenReturn(Optional.empty());
        when(reputationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newShopWithNoSalesHasNonNegativeScore() {
        stubOrders(0, 0, null);

        Reputation rep = service.recalculate(shop);

        assertThat(rep.getScore()).isBetween(0, 100);
    }

    @Test
    void highRatingAndManySalesProduceHighScore() {
        stubOrders(120, 120, new BigDecimal("4.8"));

        Reputation rep = service.recalculate(shop);

        assertThat(rep.getScore()).isGreaterThan(60);
    }

    @Test
    void highIncidentRateLowersScore() {
        stubOrders(50, 100, new BigDecimal("3.0"));
        int scoreWithIncidents = service.recalculate(shop).getScore();

        stubOrders(50, 50, new BigDecimal("3.0"));
        int scoreClean = service.recalculate(shop).getScore();

        assertThat(scoreWithIncidents).isLessThanOrEqualTo(scoreClean);
    }

    @Test
    void over100SalesBadgeAssignedAtThreshold() {
        stubOrders(100, 100, new BigDecimal("4.0"));

        assertThat(service.recalculate(shop).getBadges()).contains(Badge.OVER_100_SALES);
    }

    @Test
    void over100SalesBadgeNotAssignedBeforeThreshold() {
        stubOrders(99, 99, new BigDecimal("4.0"));

        assertThat(service.recalculate(shop).getBadges()).doesNotContain(Badge.OVER_100_SALES);
    }

    @Test
    void verifiedBadgeAssignedWhenShopIsVerified() {
        shop.setVerified(true);
        stubOrders(5, 5, new BigDecimal("4.0"));

        assertThat(service.recalculate(shop).getBadges()).contains(Badge.VERIFIED);
    }

    @Test
    void featuredBadgeConsistentWithScore() {
        stubOrders(100, 100, new BigDecimal("5.0"));

        Reputation rep = service.recalculate(shop);

        if (rep.getScore() > 85) {
            assertThat(rep.getBadges()).contains(Badge.FEATURED);
        } else {
            assertThat(rep.getBadges()).doesNotContain(Badge.FEATURED);
        }
    }

    @Test
    void scoreFormulaIsConsistentWithKnownValues() {
        // Manual: 0 sales, 3★, age=0, 0 incidents
        // score = 0.40*(3/5) + 0 + 0 - 0 = 0.24 → 24
        int score = service.computeScore(0, new BigDecimal("3.0"), 0, BigDecimal.ZERO);
        assertThat(score).isEqualTo(24);
    }

    @Test
    void scoreClampedToZeroForMaxPenalty() {
        // 0 rating + 100% incident rate → never negative
        int score = service.computeScore(0, BigDecimal.ZERO, 0, BigDecimal.ONE);
        assertThat(score).isEqualTo(0);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void stubOrders(long nSales, long total, BigDecimal avgRating) {
        when(orderRepository.countByShopIdAndStatus(any(), eq(OrderStatus.DELIVERED))).thenReturn(nSales);
        when(orderRepository.countByShopId(any())).thenReturn(total);
        when(reviewRepository.avgRatingByShopId(any())).thenReturn(avgRating);
    }
}
