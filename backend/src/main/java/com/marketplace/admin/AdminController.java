package com.marketplace.admin;

import com.marketplace.common.NotFoundException;
import com.marketplace.reputation.ReputationService;
import com.marketplace.review.ReviewRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopRepository;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Back-office operations (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReputationService reputationService;

    public AdminController(ShopRepository shopRepository,
                           UserRepository userRepository,
                           ReviewRepository reviewRepository,
                           ReputationService reputationService) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.reputationService = reputationService;
    }

    // ── Shops ────────────────────────────────────────────────────────────────

    @GetMapping("/shops/pending")
    @Operation(summary = "List shops pending verification")
    public List<AdminShopDto> listPendingShops() {
        return shopRepository.findByVerifiedFalse().stream()
                .map(AdminShopDto::from)
                .toList();
    }

    @GetMapping("/shops")
    @Operation(summary = "List all shops")
    public List<AdminShopDto> listAllShops() {
        return shopRepository.findAll().stream()
                .map(AdminShopDto::from)
                .toList();
    }

    @PostMapping("/shops/{id}/verify")
    @Transactional
    @Operation(summary = "Verify a seller shop")
    public AdminShopDto verifyShop(@PathVariable Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        shop.setVerified(true);
        shopRepository.save(shop);
        reputationService.recalculateForShop(id);
        return AdminShopDto.from(shop);
    }

    // ── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public List<AdminUserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserDto::from)
                .toList();
    }

    @PostMapping("/users/{id}/suspend")
    @Transactional
    @Operation(summary = "Suspend a user account")
    public AdminUserDto suspendUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setSuspended(true);
        userRepository.save(user);
        return AdminUserDto.from(user);
    }

    @PostMapping("/users/{id}/unsuspend")
    @Transactional
    @Operation(summary = "Lift suspension from a user account")
    public AdminUserDto unsuspendUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setSuspended(false);
        userRepository.save(user);
        return AdminUserDto.from(user);
    }

    // ── Reviews ──────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    @Operation(summary = "List all reviews, most recent first")
    public List<AdminReviewDto> listReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminReviewDto::from)
                .toList();
    }

    @DeleteMapping("/reviews/{id}")
    @Transactional
    @Operation(summary = "Moderate (delete) a reported review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found");
        }
        reviewRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
