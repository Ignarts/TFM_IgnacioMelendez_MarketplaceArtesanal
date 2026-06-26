package com.marketplace.reputation;

public enum Badge {
    VERIFIED,       // shop.verified = true (set by admin)
    FEATURED,       // score > 85
    FAST_RESPONSE,  // orders attended in < 24 h
    OVER_100_SALES  // nSales >= 100
}
