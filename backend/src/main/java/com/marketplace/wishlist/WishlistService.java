package com.marketplace.wishlist;

import com.marketplace.common.NotFoundException;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    /** Saved products, newest first. Products withdrawn by moderation are left out. */
    @Transactional(readOnly = true)
    public List<Product> list(User user) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WishlistItem::getProduct)
                .filter(p -> !p.isHidden())
                .toList();
    }

    /** Idempotent: saving an already saved product is a no-op. */
    @Transactional
    public void add(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> !p.isHidden())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepository.save(new WishlistItem(user.getId(), product));
        }
    }

    @Transactional
    public void remove(User user, Long productId) {
        wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(wishlistRepository::delete);
    }
}
