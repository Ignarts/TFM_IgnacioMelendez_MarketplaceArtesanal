package com.marketplace.reputation;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Derived aggregate materialised by ReputationService on a schedule.
 * Does not own domain truth — all data comes from Order/Review/Shop.
 */
@Entity
@Table(name = "reputations")
public class Reputation {

    @Id
    private Long shopId;

    /** 0–100 composite score (see ReputationService for formula and weights). */
    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int nSales;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ageDays;

    /** Proportion of orders with incidents/cancellations ∈ [0,1]. */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal incidentRate = BigDecimal.ZERO;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reputation_badges", joinColumns = @JoinColumn(name = "shop_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "badge")
    private Set<Badge> badges = new HashSet<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Reputation() {}

    public Reputation(Long shopId) {
        this.shopId = shopId;
    }

    public Long getShopId() { return shopId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getNSales() { return nSales; }
    public void setNSales(int nSales) { this.nSales = nSales; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public int getAgeDays() { return ageDays; }
    public void setAgeDays(int ageDays) { this.ageDays = ageDays; }

    public BigDecimal getIncidentRate() { return incidentRate; }
    public void setIncidentRate(BigDecimal incidentRate) { this.incidentRate = incidentRate; }

    public Set<Badge> getBadges() { return badges; }
    public void setBadges(Set<Badge> badges) { this.badges = badges; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
