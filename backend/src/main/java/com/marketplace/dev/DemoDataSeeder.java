package com.marketplace.dev;

import com.marketplace.category.Category;
import com.marketplace.category.CategoryRepository;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.shop.Shop;
import com.marketplace.shop.ShopRepository;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Seeds fake shops and products for demos/manual review. Disabled by default; enable with
 * {@code APP_SEED_DEMO=true} (env) or {@code app.seed-demo=true}. Never runs in tests.
 * Idempotent: skips if the first demo seller already exists. All demo sellers share the
 * password "password123".
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, ShopRepository shopRepository,
                          ProductRepository productRepository, CategoryRepository categoryRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Each demo shop: display name, owner name, category slug, and its product titles.
    private record ShopSeed(String shopName, String ownerName, String categorySlug, List<String> products) {}

    private static final List<ShopSeed> SHOPS = List.of(
            new ShopSeed("Cerámica Ana", "Ana Ruiz", "ceramica",
                    List.of("Vasija esmaltada", "Cuenco rústico", "Plato de autor", "Jarrón azul cobalto",
                            "Taza artesanal", "Tetera de barro", "Set de cuencos", "Botijo tradicional")),
            new ShopSeed("Torno y Tierra", "Marcos León", "ceramica",
                    List.of("Maceta torneada", "Fuente decorativa", "Cántaro grande", "Plato hondo gres",
                            "Bol japonés", "Florero minimalista", "Cenicero artesanal")),
            new ShopSeed("Joyas del Sur", "Lucía Mora", "joyeria",
                    List.of("Anillo de plata", "Pendientes de aro", "Collar de ámbar", "Pulsera trenzada",
                            "Gargantilla minimal", "Broche floral", "Colgante de cuarzo", "Anillo con turquesa")),
            new ShopSeed("Forja Fina", "David Soto", "joyeria",
                    List.of("Brazalete martillado", "Pendientes de cobre", "Anillo sello", "Cadena de eslabones",
                            "Tobillera de plata", "Gemelos artesanos")),
            new ShopSeed("Cuero Noble", "Elena Vidal", "cuero",
                    List.of("Cartera de piel", "Cinturón curtido", "Bolso bandolera", "Funda de gafas",
                            "Llavero de cuero", "Mochila vintage", "Estuche de viaje", "Monedero plegable")),
            new ShopSeed("Taller del Lápiz", "Pablo Gil", "ilustracion",
                    List.of("Lámina botánica", "Retrato a tinta", "Póster geométrico", "Set de postales",
                            "Ilustración de ciudad", "Acuarela floral", "Cómic ilustrado")),
            new ShopSeed("Hilo y Telar", "Sara Núñez", "textil",
                    List.of("Manta de lana", "Bufanda tejida", "Cojín bordado", "Tapiz mural",
                            "Camino de mesa", "Cesto de tela", "Funda de cojín kilim", "Chal de algodón")),
            new ShopSeed("Punto Bravo", "Iván Costa", "textil",
                    List.of("Jersey de punto", "Calcetines de lana", "Gorro tejido", "Guantes de invierno",
                            "Poncho artesanal")),
            new ShopSeed("Madera Viva", "Carla Prado", "madera",
                    List.of("Tabla de cortar", "Cuchara tallada", "Caja joyero", "Reloj de pared",
                            "Set de posavasos", "Bandeja de roble", "Banco bajo", "Marco de fotos")),
            new ShopSeed("Raíz y Veta", "Hugo Marín", "madera",
                    List.of("Lámpara de nogal", "Estantería flotante", "Perchero de pared", "Soporte de móvil",
                            "Taburete artesano", "Organizador de escritorio"))
    );

    @Override
    public void run(ApplicationArguments args) {
        String firstOwnerEmail = ownerEmail(SHOPS.get(0));
        if (userRepository.existsByEmail(firstOwnerEmail)) {
            return; // already seeded
        }

        Map<String, Category> categoriesBySlug = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getSlug, c -> c));

        // Deterministic pseudo-random so re-seeding a fresh DB gives the same catalog.
        Random random = new Random(42);
        String hash = passwordEncoder.encode("password123");
        int productCount = 0;

        for (ShopSeed seed : SHOPS) {
            Category category = categoriesBySlug.get(seed.categorySlug());
            if (category == null) continue;

            User owner = userRepository.save(new User(
                    ownerEmail(seed), hash, seed.ownerName(), EnumSet.of(Role.BUYER, Role.SELLER)));

            Shop shop = shopRepository.save(new Shop(owner.getId(), seed.shopName(),
                    "Productos artesanales de " + seed.shopName() + "."));
            shop.setVerified(random.nextBoolean());
            shopRepository.save(shop);

            for (String title : seed.products()) {
                Product product = new Product();
                product.setShop(shop);
                product.setCategory(category);
                product.setTitle(title);
                product.setDescription(title + " hecho a mano por " + seed.ownerName() + ".");
                product.setPrice(BigDecimal.valueOf(8 + random.nextInt(92) + random.nextInt(100) / 100.0)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
                product.setStock(1 + random.nextInt(40));
                product.setImages(List.of(
                        "https://picsum.photos/seed/" + seed.categorySlug() + productCount + "/600/450",
                        "https://picsum.photos/seed/" + seed.categorySlug() + (productCount + 100) + "/600/450"));
                productRepository.save(product);
                productCount++;
            }
        }
    }

    private static String ownerEmail(ShopSeed seed) {
        return seed.shopName().toLowerCase().replaceAll("[^a-z0-9]+", ".") + "@demo.artesanal";
    }
}
