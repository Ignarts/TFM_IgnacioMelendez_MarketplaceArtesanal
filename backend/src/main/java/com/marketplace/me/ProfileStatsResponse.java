package com.marketplace.me;

import java.math.BigDecimal;

/** Aggregated KPIs shown on the user's profile. {@code seller} is null for non-sellers. */
public record ProfileStatsResponse(
        int purchaseCount,
        long uniqueProductsBought,
        long starsGiven,
        SellerStats seller
) {
    public record SellerStats(
            long productsSold,
            long reviewsReceived,
            BigDecimal avgRating,
            long productsOnSale
    ) {}
}
