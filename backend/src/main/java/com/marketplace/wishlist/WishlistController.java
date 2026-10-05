package com.marketplace.wishlist;

import com.marketplace.product.dto.ProductResponse;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/wishlist")
@Tag(name = "Wishlist", description = "Favourite products of the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List favourite products")
    public List<ProductResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return wishlistService.list(currentUser(principal)).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Add a product to favourites")
    public ResponseEntity<Void> add(@AuthenticationPrincipal UserDetails principal, @PathVariable Long productId) {
        wishlistService.add(currentUser(principal), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from favourites")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserDetails principal, @PathVariable Long productId) {
        wishlistService.remove(currentUser(principal), productId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(UserDetails principal) {
        return userService.getByEmail(principal.getUsername());
    }
}
