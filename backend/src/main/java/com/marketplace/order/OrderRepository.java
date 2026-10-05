package com.marketplace.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);

    long countByShopIdAndStatus(Long shopId, OrderStatus status);

    long countByShopId(Long shopId);

    boolean existsByItems_Product_Id(Long productId);
}
