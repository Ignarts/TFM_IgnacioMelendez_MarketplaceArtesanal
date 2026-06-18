package com.marketplace.category;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds the fixed set of craft categories on startup if the table is empty. */
@Component
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.saveAll(List.of(
                new Category("Cerámica", "ceramica"),
                new Category("Joyería", "joyeria"),
                new Category("Cuero", "cuero"),
                new Category("Ilustración", "ilustracion"),
                new Category("Textil", "textil"),
                new Category("Madera", "madera")
        ));
    }
}
