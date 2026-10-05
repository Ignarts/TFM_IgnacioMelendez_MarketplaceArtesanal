package com.marketplace.order;

import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.order.dto.CheckoutRequest;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopService shopService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        ShopService shopService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.shopService = shopService;
    }

    /**
     * Simulated checkout: validates stock, freezes prices and splits the cart into one order
     * per shop. Stock is decremented now and not restocked (there is no cancel flow yet).
     */
    @Transactional
    public List<Order> checkout(User buyer, CheckoutRequest request) {
        // Group lines by shop so each order belongs to a single seller.
        Map<Long, Order> ordersByShop = new LinkedHashMap<>();

        for (CheckoutRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + line.productId()));

            if (product.isHidden()) {
                throw new ConflictException("Product is no longer available: " + product.getTitle());
            }
            if (product.getStock() < line.quantity()) {
                throw new ConflictException("Not enough stock for: " + product.getTitle());
            }
            product.setStock(product.getStock() - line.quantity());

            Long shopId = product.getShop().getId();
            Order order = ordersByShop.computeIfAbsent(shopId, id -> new Order(buyer.getId(), id));
            order.addItem(new OrderItem(product, product.getPrice(), line.quantity()));
        }

        return orderRepository.saveAll(ordersByShop.values());
    }

    public List<Order> myOrders(User buyer) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId());
    }

    public List<Order> shopOrders(User seller) {
        Shop shop = shopService.getByOwner(seller);
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
    }

    /** Simulated payment: PENDING → PAID. */
    @Transactional
    public Order pay(User buyer, Long orderId) {
        Order order = ownedByBuyer(buyer, orderId);
        transition(order, OrderStatus.PENDING, OrderStatus.PAID);
        return order;
    }

    /**
     * Buyer confirms receipt: PAID or SHIPPED → DELIVERED (this unlocks reviews).
     * Both states are accepted because the seller's shipping step is optional in the
     * simulated flow, so a buyer must be able to confirm arrival straight from PAID.
     */
    @Transactional
    public Order confirmReceipt(User buyer, Long orderId) {
        Order order = ownedByBuyer(buyer, orderId);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new ConflictException("Order must be PAID or SHIPPED to become DELIVERED");
        }
        order.setStatus(OrderStatus.DELIVERED);
        return order;
    }

    /** Seller ships the order: PAID → SHIPPED. Ownership rule via the order's shop. */
    @Transactional
    public Order ship(User seller, Long orderId) {
        Shop shop = shopService.getByOwner(seller);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getShopId().equals(shop.getId())) {
            throw new AccessDeniedException("You cannot manage orders from another shop");
        }
        transition(order, OrderStatus.PAID, OrderStatus.SHIPPED);
        return order;
    }

    private Order ownedByBuyer(User buyer, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getBuyerId().equals(buyer.getId())) {
            throw new AccessDeniedException("This order belongs to another buyer");
        }
        return order;
    }

    private void transition(Order order, OrderStatus expected, OrderStatus next) {
        if (order.getStatus() != expected) {
            throw new ConflictException("Order must be " + expected + " to become " + next);
        }
        order.setStatus(next);
    }

    /** True if the buyer has a delivered order containing the product (verified-purchase rule). */
    public boolean hasDeliveredProduct(Long buyerId, Long productId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(i -> i.getProduct().getId().equals(productId));
    }
}
