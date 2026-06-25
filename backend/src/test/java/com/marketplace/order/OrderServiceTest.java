package com.marketplace.order;

import com.marketplace.common.ConflictException;
import com.marketplace.product.ProductRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ShopService shopService;
    @InjectMocks private OrderService orderService;

    @Test
    void shipRejectsOrderFromAnotherShop() {
        User seller = mock(User.class);
        Shop shop = mock(Shop.class);
        when(shop.getId()).thenReturn(1L);
        when(shopService.getByOwner(seller)).thenReturn(shop);

        Order order = new Order(7L, 2L); // belongs to shop 2, not the seller's shop 1
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> orderService.ship(seller, 99L));
    }

    @Test
    void payRejectsOrderThatIsNotPending() {
        User buyer = mock(User.class);
        when(buyer.getId()).thenReturn(7L);

        Order order = new Order(7L, 2L);
        order.setStatus(OrderStatus.SHIPPED); // not PENDING
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThrows(ConflictException.class, () -> orderService.pay(buyer, 5L));
    }
}
