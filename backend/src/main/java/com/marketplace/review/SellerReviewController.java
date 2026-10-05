package com.marketplace.review;

import com.marketplace.review.dto.ReviewReplyRequest;
import com.marketplace.review.dto.ReviewResponse;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/reviews")
@Tag(name = "Seller reviews", description = "Reviews received by my shop and my replies")
@SecurityRequirement(name = "bearerAuth")
public class SellerReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public SellerReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List reviews of my shop's products")
    public List<ReviewResponse> list(@AuthenticationPrincipal UserDetails principal) {
        User seller = userService.getByEmail(principal.getUsername());
        return reviewService.listForSeller(seller).stream().map(ReviewResponse::from).toList();
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "Reply to (or edit the reply of) a review of my shop (ownership rule)")
    public ReviewResponse reply(@AuthenticationPrincipal UserDetails principal,
                                @PathVariable Long id,
                                @Valid @RequestBody ReviewReplyRequest request) {
        User seller = userService.getByEmail(principal.getUsername());
        return ReviewResponse.from(reviewService.reply(seller, id, request.reply()));
    }
}
