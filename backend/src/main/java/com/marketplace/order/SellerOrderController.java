package com.marketplace.order;

import com.marketplace.order.dto.OrderResponse;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@Tag(name = "Seller orders", description = "Orders received by my shop")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderController {

    private final OrderService orderService;
    private final UserService userService;

    public SellerOrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List orders received by my shop")
    public List<OrderResponse> list(@AuthenticationPrincipal UserDetails principal) {
        User seller = userService.getByEmail(principal.getUsername());
        return orderService.shopOrders(seller).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "Mark an order as shipped: PAID → SHIPPED (ownership rule)")
    public OrderResponse ship(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User seller = userService.getByEmail(principal.getUsername());
        return OrderResponse.from(orderService.ship(seller, id));
    }
}
