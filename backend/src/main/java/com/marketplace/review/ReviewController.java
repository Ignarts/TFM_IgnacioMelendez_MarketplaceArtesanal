package com.marketplace.review;

import com.marketplace.review.dto.ReviewRequest;
import com.marketplace.review.dto.ReviewResponse;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@Tag(name = "Reviews", description = "Verified product reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List reviews of a product (public)")
    public List<ReviewResponse> list(@PathVariable Long productId) {
        return reviewService.listForProduct(productId).stream().map(ReviewResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Review a product you bought", security = @SecurityRequirement(name = "bearerAuth"))
    public ReviewResponse create(@AuthenticationPrincipal UserDetails principal,
                                 @PathVariable Long productId,
                                 @Valid @RequestBody ReviewRequest request) {
        User buyer = userService.getByEmail(principal.getUsername());
        return ReviewResponse.from(reviewService.create(buyer, productId, request));
    }
}
