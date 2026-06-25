package com.marketplace.order;

import com.marketplace.order.dto.CheckoutRequest;
import com.marketplace.order.dto.OrderResponse;
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
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Buyer cart checkout and order history")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout the cart (creates one order per shop)")
    public List<OrderResponse> checkout(@AuthenticationPrincipal UserDetails principal,
                                        @Valid @RequestBody CheckoutRequest request) {
        User buyer = userService.getByEmail(principal.getUsername());
        return orderService.checkout(buyer, request).stream().map(OrderResponse::from).toList();
    }

    @GetMapping
    @Operation(summary = "My order history")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal UserDetails principal) {
        User buyer = userService.getByEmail(principal.getUsername());
        return orderService.myOrders(buyer).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an order (simulated): PENDING → PAID")
    public OrderResponse pay(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User buyer = userService.getByEmail(principal.getUsername());
        return OrderResponse.from(orderService.pay(buyer, id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm receipt: SHIPPED → DELIVERED (unlocks reviews)")
    public OrderResponse confirm(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User buyer = userService.getByEmail(principal.getUsername());
        return OrderResponse.from(orderService.confirmReceipt(buyer, id));
    }
}
