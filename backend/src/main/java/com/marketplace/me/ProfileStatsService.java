package com.marketplace.me;

import com.marketplace.order.OrderService;
import com.marketplace.order.OrderStatus;
import com.marketplace.product.ProductRepository;
import com.marketplace.review.Review;
import com.marketplace.review.ReviewRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProfileStatsService {

    private final OrderService orderService;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ShopService shopService;

    public ProfileStatsService(OrderService orderService, ReviewRepository reviewRepository,
                               ProductRepository productRepository, ShopService shopService) {
        this.orderService = orderService;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.shopService = shopService;
    }

    @Transactional(readOnly = true)
    public ProfileStatsResponse forUser(User user) {
        var orders = orderService.myOrders(user);
        int purchaseCount = orders.size();
        long uniqueProductsBought = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getProduct().getId())
                .distinct()
                .count();
        long starsGiven = reviewRepository.findByBuyerId(user.getId()).stream()
                .mapToInt(Review::getRating)
                .sum();

        ProfileStatsResponse.SellerStats seller = null;
        if (user.getRoles().contains(Role.SELLER)) {
            Shop shop = shopService.getByOwner(user);
            // Only orders that were at least paid count as actual sales.
            long productsSold = orderService.shopOrders(user).stream()
                    .filter(o -> o.getStatus() != OrderStatus.PENDING)
                    .flatMap(o -> o.getItems().stream())
                    .mapToLong(i -> i.getQuantity())
                    .sum();
            long reviewsReceived = reviewRepository.countByProduct_ShopId(shop.getId());
            BigDecimal avg = reviewRepository.avgRatingByShopId(shop.getId());
            BigDecimal avgRating = avg == null ? BigDecimal.ZERO : avg.setScale(2, RoundingMode.HALF_UP);
            long productsOnSale = productRepository.countByShopId(shop.getId());
            seller = new ProfileStatsResponse.SellerStats(productsSold, reviewsReceived, avgRating, productsOnSale);
        }

        return new ProfileStatsResponse(purchaseCount, uniqueProductsBought, starsGiven, seller);
    }
}
