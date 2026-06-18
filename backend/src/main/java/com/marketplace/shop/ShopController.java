package com.marketplace.shop;

import com.marketplace.shop.dto.CreateShopRequest;
import com.marketplace.shop.dto.ShopResponse;
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

@RestController
@RequestMapping("/api/seller/shop")
@Tag(name = "Shop", description = "Gestión de la tienda del vendedor")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {

    private final ShopService shopService;
    private final UserService userService;

    public ShopController(ShopService shopService, UserService userService) {
        this.shopService = shopService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir tienda (otorga el rol SELLER; requiere volver a iniciar sesión)")
    public ShopResponse openShop(@AuthenticationPrincipal UserDetails principal,
                                 @Valid @RequestBody CreateShopRequest request) {
        User owner = userService.getByEmail(principal.getUsername());
        return ShopResponse.from(shopService.openShop(owner, request));
    }

    @GetMapping
    @Operation(summary = "Obtener la tienda del usuario autenticado")
    public ShopResponse myShop(@AuthenticationPrincipal UserDetails principal) {
        User owner = userService.getByEmail(principal.getUsername());
        return ShopResponse.from(shopService.getByOwner(owner));
    }
}
