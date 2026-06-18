package com.marketplace.product;

import com.marketplace.category.Category;
import com.marketplace.category.CategoryRepository;
import com.marketplace.common.NotFoundException;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopService;
import com.marketplace.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopService shopService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ShopService shopService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.shopService = shopService;
    }

    @PreAuthorize("hasRole('SELLER')")
    public List<Product> listOwn(User seller) {
        Shop shop = shopService.getByOwner(seller);
        return productRepository.findByShopId(shop.getId());
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public Product create(User seller, ProductRequest request) {
        Shop shop = shopService.getByOwner(seller);
        Product product = new Product();
        product.setShop(shop);
        apply(product, request);
        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public Product update(User seller, Long productId, ProductRequest request) {
        Product product = getOwned(seller, productId);
        apply(product, request);
        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public void delete(User seller, Long productId) {
        Product product = getOwned(seller, productId);
        productRepository.delete(product);
    }

    /** Loads a product and enforces the ownership rule: it must belong to the seller's shop. */
    private Product getOwned(User seller, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        if (!product.getShop().getOwnerId().equals(seller.getId())) {
            throw new AccessDeniedException("No puedes gestionar productos de otra tienda");
        }
        return product;
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        product.setCategory(category);
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImages(request.images() == null ? new ArrayList<>() : new ArrayList<>(request.images()));
    }
}
