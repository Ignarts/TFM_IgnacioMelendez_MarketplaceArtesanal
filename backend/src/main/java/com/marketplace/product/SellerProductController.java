package com.marketplace.product;

import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
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
@RequestMapping("/api/seller/products")
@Tag(name = "Seller products", description = "Seller product CRUD")
@SecurityRequirement(name = "bearerAuth")
public class SellerProductController {

    private final ProductService productService;
    private final UserService userService;

    public SellerProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List my shop's products")
    public List<ProductResponse> list(@AuthenticationPrincipal UserDetails principal) {
        User seller = userService.getByEmail(principal.getUsername());
        return productService.listOwn(seller).stream().map(ProductResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product in my shop")
    public ProductResponse create(@AuthenticationPrincipal UserDetails principal,
                                  @Valid @RequestBody ProductRequest request) {
        User seller = userService.getByEmail(principal.getUsername());
        return ProductResponse.from(productService.create(seller, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product of my shop (ownership rule)")
    public ProductResponse update(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request) {
        User seller = userService.getByEmail(principal.getUsername());
        return ProductResponse.from(productService.update(seller, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a product of my shop (ownership rule)")
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User seller = userService.getByEmail(principal.getUsername());
        productService.delete(seller, id);
    }
}
