package com.marketplace.review;

import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.order.OrderService;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.report.ReportService;
import com.marketplace.review.dto.ReviewRequest;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    /** Anti-fraud: this many 5★ reviews from one buyer to one shop within the window gets flagged. */
    static final int SUSPICIOUS_FIVE_STAR_REVIEWS = 3;
    static final Duration SUSPICIOUS_WINDOW = Duration.ofHours(24);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ShopService shopService;
    private final ReportService reportService;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         OrderService orderService, ShopService shopService, ReportService reportService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.shopService = shopService;
        this.reportService = reportService;
    }

    public List<Review> listForProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /** Verified-purchase rule: only a buyer with a delivered order of this product can review, once. */
    @Transactional
    public Review create(User buyer, Long productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (!orderService.hasDeliveredProduct(buyer.getId(), productId)) {
            throw new AccessDeniedException("Only buyers with a delivered order can review this product");
        }
        if (reviewRepository.existsByProductIdAndBuyerId(productId, buyer.getId())) {
            throw new ConflictException("You already reviewed this product");
        }

        Review review = reviewRepository.save(
                new Review(product, buyer.getId(), buyer.getName(), request.rating(), request.comment()));
        if (review.getRating() == 5) {
            flagIfSuspicious(review, product.getShop().getId());
        }
        return review;
    }

    /** A burst of 5★ reviews is not rejected (it may be genuine) but queued for admin moderation. */
    private void flagIfSuspicious(Review review, Long shopId) {
        long recent = reviewRepository.countRecentFiveStarReviewsByBuyerInShop(
                shopId, review.getBuyerId(), Instant.now().minus(SUSPICIOUS_WINDOW));
        if (recent >= SUSPICIOUS_FIVE_STAR_REVIEWS) {
            reportService.flagReview(review,
                    recent + " reseñas de 5★ del mismo comprador a esta tienda en menos de 24 h");
        }
    }

    /** Reviews received by the seller's products, most recent first. */
    @PreAuthorize("hasRole('SELLER')")
    public List<Review> listForSeller(User seller) {
        Shop shop = shopService.getByOwner(seller);
        return reviewRepository.findByProduct_ShopIdOrderByCreatedAtDesc(shop.getId());
    }

    /** Ownership rule: a seller can only answer reviews of products from their own shop. */
    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public Review reply(User seller, Long reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!review.getProduct().getShop().getOwnerId().equals(seller.getId())) {
            throw new AccessDeniedException("You can only reply to reviews of your own products");
        }
        review.setSellerReply(reply.trim());
        review.setSellerReplyAt(Instant.now());
        return reviewRepository.save(review);
    }
}
