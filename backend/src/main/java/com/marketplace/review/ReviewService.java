package com.marketplace.review;

import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.order.OrderService;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.review.dto.ReviewRequest;
import com.marketplace.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         OrderService orderService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
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

        return reviewRepository.save(
                new Review(product, buyer.getId(), buyer.getName(), request.rating(), request.comment()));
    }
}
