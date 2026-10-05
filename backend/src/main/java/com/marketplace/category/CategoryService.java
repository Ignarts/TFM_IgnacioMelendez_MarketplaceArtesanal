package com.marketplace.category;

import com.marketplace.category.dto.CategoryRequest;
import com.marketplace.common.BadRequestException;
import com.marketplace.common.ConflictException;
import com.marketplace.common.NotFoundException;
import com.marketplace.product.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

/** Global category management (ADMIN back-office). */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category create(CategoryRequest request) {
        String slug = slugOf(request);
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }
        return categoryRepository.save(new Category(request.name().trim(), slug));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = get(id);
        String slug = slugOf(request);
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }
        category.setName(request.name().trim());
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    /** A category still used by products cannot be deleted (products require a category). */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        Category category = get(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category is in use by one or more products");
        }
        categoryRepository.delete(category);
    }

    private Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    /** Uses the given slug or derives it from the name: "Cerámica fina" → "ceramica-fina". */
    static String slugOf(CategoryRequest request) {
        String source = request.slug() == null || request.slug().isBlank() ? request.name() : request.slug();
        String slug = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) {
            throw new BadRequestException("Category name must contain letters or digits");
        }
        return slug;
    }
}
