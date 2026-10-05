package com.marketplace.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByShopId(Long shopId);

    long countByShopId(Long shopId);

    long countByShopIdAndHiddenFalse(Long shopId);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findByHiddenTrue();
}
