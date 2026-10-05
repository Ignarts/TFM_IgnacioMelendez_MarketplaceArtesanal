package com.marketplace.product;

import com.marketplace.category.CategoryRepository;
import com.marketplace.order.OrderRepository;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ShopService shopService;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private ProductService productService;

    @Test
    void updateRejectsProductFromAnotherShop() {
        User seller = mock(User.class);
        when(seller.getId()).thenReturn(2L);

        Shop otherShop = mock(Shop.class);
        when(otherShop.getOwnerId()).thenReturn(1L);

        Product product = mock(Product.class);
        when(product.getShop()).thenReturn(otherShop);
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        ProductRequest request = new ProductRequest("Bowl", null, BigDecimal.TEN, 3, 1L, null);

        assertThrows(AccessDeniedException.class, () -> productService.update(seller, 99L, request));
        verify(productRepository, never()).save(any());
    }
}
