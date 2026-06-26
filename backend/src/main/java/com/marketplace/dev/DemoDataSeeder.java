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
import org.springframework.core.annotation.Order;
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
@Order(2) // after CategorySeeder, which this depends on
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

    // A product: Spanish display title + an English keyword used to fetch a matching photo.
    private record Item(String title, String keyword) {}

    // Each demo shop: display name, owner name, category slug, and its products.
    private record ShopSeed(String shopName, String ownerName, String categorySlug, List<Item> products) {}

    private static final List<ShopSeed> SHOPS = List.of(
            new ShopSeed("Cerámica Ana", "Ana Ruiz", "ceramica", List.of(
                    new Item("Vasija esmaltada", "vase"), new Item("Cuenco rústico", "bowl"),
                    new Item("Plato de autor", "plate"), new Item("Jarrón azul cobalto", "vase"),
                    new Item("Taza artesanal", "mug"), new Item("Tetera de barro", "teapot"),
                    new Item("Set de cuencos", "bowl"), new Item("Botijo tradicional", "pottery"))),
            new ShopSeed("Torno y Tierra", "Marcos León", "ceramica", List.of(
                    new Item("Maceta torneada", "pot"), new Item("Fuente decorativa", "pottery"),
                    new Item("Cántaro grande", "pitcher"), new Item("Plato hondo gres", "plate"),
                    new Item("Bol japonés", "bowl"), new Item("Florero minimalista", "vase"),
                    new Item("Cenicero artesanal", "ashtray"))),
            new ShopSeed("Joyas del Sur", "Lucía Mora", "joyeria", List.of(
                    new Item("Anillo de plata", "ring"), new Item("Pendientes de aro", "earrings"),
                    new Item("Collar de ámbar", "necklace"), new Item("Pulsera trenzada", "bracelet"),
                    new Item("Gargantilla minimal", "necklace"), new Item("Broche floral", "brooch"),
                    new Item("Colgante de cuarzo", "pendant"), new Item("Anillo con turquesa", "ring"))),
            new ShopSeed("Forja Fina", "David Soto", "joyeria", List.of(
                    new Item("Brazalete martillado", "bracelet"), new Item("Pendientes de cobre", "earrings"),
                    new Item("Anillo sello", "ring"), new Item("Cadena de eslabones", "necklace"),
                    new Item("Tobillera de plata", "anklet"), new Item("Gemelos artesanos", "cufflinks"))),
            new ShopSeed("Cuero Noble", "Elena Vidal", "cuero", List.of(
                    new Item("Cartera de piel", "wallet"), new Item("Cinturón curtido", "belt"),
                    new Item("Bolso bandolera", "handbag"), new Item("Funda de gafas", "case"),
                    new Item("Llavero de cuero", "keychain"), new Item("Mochila vintage", "backpack"),
                    new Item("Estuche de viaje", "case"), new Item("Monedero plegable", "purse"))),
            new ShopSeed("Taller del Lápiz", "Pablo Gil", "ilustracion", List.of(
                    new Item("Lámina botánica", "illustration"), new Item("Retrato a tinta", "portrait"),
                    new Item("Póster geométrico", "poster"), new Item("Set de postales", "postcard"),
                    new Item("Ilustración de ciudad", "drawing"), new Item("Acuarela floral", "watercolor"),
                    new Item("Cómic ilustrado", "comic"))),
            new ShopSeed("Hilo y Telar", "Sara Núñez", "textil", List.of(
                    new Item("Manta de lana", "blanket"), new Item("Bufanda tejida", "scarf"),
                    new Item("Cojín bordado", "cushion"), new Item("Tapiz mural", "tapestry"),
                    new Item("Camino de mesa", "tablecloth"), new Item("Cesto de tela", "basket"),
                    new Item("Funda de cojín kilim", "cushion"), new Item("Chal de algodón", "shawl"))),
            new ShopSeed("Punto Bravo", "Iván Costa", "textil", List.of(
                    new Item("Jersey de punto", "sweater"), new Item("Calcetines de lana", "wool,socks"),
                    new Item("Gorro tejido", "knitted,hat"), new Item("Guantes de invierno", "winter,gloves"),
                    new Item("Poncho artesanal", "poncho"))),
            new ShopSeed("Madera Viva", "Carla Prado", "madera", List.of(
                    new Item("Tabla de cortar", "choppingboard"), new Item("Cuchara tallada", "spoon"),
                    new Item("Caja joyero", "box"), new Item("Reloj de pared", "clock"),
                    new Item("Set de posavasos", "coaster"), new Item("Bandeja de roble", "tray"),
                    new Item("Banco bajo", "bench"), new Item("Marco de fotos", "frame"))),
            new ShopSeed("Raíz y Veta", "Hugo Marín", "madera", List.of(
                    new Item("Lámpara de nogal", "lamp"), new Item("Estantería flotante", "shelf"),
                    new Item("Perchero de pared", "coatrack"), new Item("Soporte de móvil", "stand"),
                    new Item("Taburete artesano", "stool"), new Item("Organizador de escritorio", "organizer")))
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

        for (ShopSeed seed : SHOPS) {
            Category category = categoriesBySlug.get(seed.categorySlug());
            if (category == null) continue;

            User owner = userRepository.save(new User(
                    ownerEmail(seed), hash, seed.ownerName(), EnumSet.of(Role.BUYER, Role.SELLER)));

            Shop shop = shopRepository.save(new Shop(owner.getId(), seed.shopName(),
                    "Productos artesanales de " + seed.shopName() + "."));
            shop.setVerified(random.nextBoolean());
            shopRepository.save(shop);

            for (Item item : seed.products()) {
                Product product = new Product();
                product.setShop(shop);
                product.setCategory(category);
                product.setTitle(item.title());
                product.setDescription(item.title() + " hecho a mano por " + seed.ownerName() + ".");
                product.setPrice(BigDecimal.valueOf(8 + random.nextInt(92) + random.nextInt(100) / 100.0)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
                product.setStock(1 + random.nextInt(40));
                product.setImages(List.of(imageFor(item.keyword())));
                productRepository.save(product);
            }
        }
    }

    private static String ownerEmail(ShopSeed seed) {
        return seed.shopName().toLowerCase().replaceAll("[^a-z0-9]+", ".") + "@demo.artesanal";
    }

    private static String imageFor(String keyword) {
        // Falls back to a labelled placeholder if a keyword ever lacks a curated image.
        return IMAGE_BY_KEYWORD.getOrDefault(keyword,
                "https://placehold.co/600x450/5b2a4e/ffffff?text=" + keyword);
    }

    // Real, on-theme photos from Wikimedia Commons (stable CDN), one per product keyword.
    private static final Map<String, String> IMAGE_BY_KEYWORD = Map.ofEntries(
            Map.entry("vase", "https://upload.wikimedia.org/wikipedia/commons/0/0f/Tulip-shaped_ceramic_vase%2C_Karanovo_culture%2C_Bulgaria%2C_6th_millennium_BC.jpg"),
            Map.entry("bowl", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Light_green_ceramic_bowl_1.jpg/960px-Light_green_ceramic_bowl_1.jpg"),
            Map.entry("plate", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f4/Byzantium%2C_12th_century_-_Deep_Plate_with_Decorative_Patterns_-_1967.137_-_Cleveland_Museum_of_Art.tif/lossy-page1-960px-Byzantium%2C_12th_century_-_Deep_Plate_with_Decorative_Patterns_-_1967.137_-_Cleveland_Museum_of_Art.tif.jpg"),
            Map.entry("mug", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Mug_Wikipedia_Malayalam.jpg/960px-Mug_Wikipedia_Malayalam.jpg"),
            Map.entry("teapot", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Meissen_Porcelain_Factory_%28German%29_-_Teapot_-_2009.105.a_-_Cleveland_Museum_of_Art.jpg/960px-Meissen_Porcelain_Factory_%28German%29_-_Teapot_-_2009.105.a_-_Cleveland_Museum_of_Art.jpg"),
            Map.entry("pottery", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/03/Handmade_Pottery_Plates_in_Marrakesh%2C_Morocco.jpg/960px-Handmade_Pottery_Plates_in_Marrakesh%2C_Morocco.jpg"),
            Map.entry("pot", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/03/Handmade_Pottery_Plates_in_Marrakesh%2C_Morocco.jpg/960px-Handmade_Pottery_Plates_in_Marrakesh%2C_Morocco.jpg"),
            Map.entry("pitcher", "https://upload.wikimedia.org/wikipedia/commons/e/ea/Pitcher-UnionPorcelain-BMA.jpg"),
            Map.entry("ashtray", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Chivas_Regal_ceramic_ashtray.jpg/960px-Chivas_Regal_ceramic_ashtray.jpg"),
            Map.entry("ring", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Silver_ring_with_unicursal_hexagram_as_used_by_Aleister_Crowley.jpg/960px-Silver_ring_with_unicursal_hexagram_as_used_by_Aleister_Crowley.jpg"),
            Map.entry("earrings", "https://upload.wikimedia.org/wikipedia/commons/8/87/Minoan_gold_earring2008.jpg"),
            Map.entry("necklace", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/Neolithic_talc_necklace_-_PRE.2009.0.237.1.IMG_1833-black.jpg/960px-Neolithic_talc_necklace_-_PRE.2009.0.237.1.IMG_1833-black.jpg"),
            Map.entry("bracelet", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Parure_of_jewellery%2C_with_emerald_necklace%2C_bracelet%2C_and_brooches.jpg/960px-Parure_of_jewellery%2C_with_emerald_necklace%2C_bracelet%2C_and_brooches.jpg"),
            Map.entry("brooch", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Roscrea_Brooch.jpg/960px-Roscrea_Brooch.jpg"),
            Map.entry("pendant", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/02021_0211-001_pendant%2C_between_12th_and_13th_century%2C_Krak%C3%B3w.jpg/960px-02021_0211-001_pendant%2C_between_12th_and_13th_century%2C_Krak%C3%B3w.jpg"),
            Map.entry("anklet", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/02022_0007_Iron_anklet%2C_Iron_Age_Hallstatt_Culture_a_bracelet.jpg/960px-02022_0007_Iron_anklet%2C_Iron_Age_Hallstatt_Culture_a_bracelet.jpg"),
            Map.entry("cufflinks", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Cufflinks-awi_hg.jpg/960px-Cufflinks-awi_hg.jpg"),
            Map.entry("wallet", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Aarong_leather_wallet.jpg/960px-Aarong_leather_wallet.jpg"),
            Map.entry("belt", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Germany_Belt-and-Buckle-01.jpg/960px-Germany_Belt-and-Buckle-01.jpg"),
            Map.entry("handbag", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Kelly_Bag.jpg/960px-Kelly_Bag.jpg"),
            Map.entry("case", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Kelly_Bag.jpg/960px-Kelly_Bag.jpg"),
            Map.entry("keychain", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Aarong_leather_wallet.jpg/960px-Aarong_leather_wallet.jpg"),
            Map.entry("backpack", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Leather_backpack_Asian_Art_Museum_SF_2006.3.692.JPG/960px-Leather_backpack_Asian_Art_Museum_SF_2006.3.692.JPG"),
            Map.entry("purse", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Leather_coin_purse.jpg/960px-Leather_coin_purse.jpg"),
            Map.entry("illustration", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/Botanical_illustration_by_Una_Weatherby_%28n%C3%A9e_Foster%29.jpg/960px-Botanical_illustration_by_Una_Weatherby_%28n%C3%A9e_Foster%29.jpg"),
            Map.entry("portrait", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Pencil_portrait_drawing.jpg/960px-Pencil_portrait_drawing.jpg"),
            Map.entry("poster", "https://upload.wikimedia.org/wikipedia/commons/b/b2/A_vintage_show_poster_advertising_Mae_West%E2%80%99s_controversial_1926_play_%E2%80%98Sex_%E2%80%99.jpg"),
            Map.entry("postcard", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Your_order_of_._._._-_Postcard_%284330149158%29.jpg/960px-Your_order_of_._._._-_Postcard_%284330149158%29.jpg"),
            Map.entry("drawing", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Pencil_drawing_of_a_dancing_girl.jpg/960px-Pencil_drawing_of_a_dancing_girl.jpg"),
            Map.entry("watercolor", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Julian_Onderdonk_-_Yellow_Flowers_on_a_Grey_Day_%281911%29.jpg/960px-Julian_Onderdonk_-_Yellow_Flowers_on_a_Grey_Day_%281911%29.jpg"),
            Map.entry("comic", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/Illustration_of_moth%2C_hawkmoth_larva%2C_snail%2C_cricket%2C_spider_from_F._A._S._Reid%27s_Comic_Insects_1882_drawn_by_Berry_F._Berry.jpg/960px-Illustration_of_moth%2C_hawkmoth_larva%2C_snail%2C_cricket%2C_spider_from_F._A._S._Reid%27s_Comic_Insects_1882_drawn_by_Berry_F._Berry.jpg"),
            Map.entry("blanket", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Classic_Navajo_Woman%27s_Shoulder_Twill-Woven_Blanket.jpg/960px-Classic_Navajo_Woman%27s_Shoulder_Twill-Woven_Blanket.jpg"),
            Map.entry("scarf", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Knit_cap_and_loop_scarf_set.jpg/960px-Knit_cap_and_loop_scarf_set.jpg"),
            Map.entry("cushion", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fb/Chalice_Veil_Made_into_a_Cushion_MET_sf-rlc-1975.1.1820.jpg/960px-Chalice_Veil_Made_into_a_Cushion_MET_sf-rlc-1975.1.1820.jpg"),
            Map.entry("tapestry", "https://upload.wikimedia.org/wikipedia/commons/7/71/TAPESTRY_WEAVE_ON_A_FRAME_LOOM.jpg"),
            Map.entry("tablecloth", "https://upload.wikimedia.org/wikipedia/commons/8/8c/Table_runner_MET_205777.jpg"),
            Map.entry("basket", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Hand_woven_basket_01.jpg/960px-Hand_woven_basket_01.jpg"),
            Map.entry("shawl", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/Yellow_cotton_Veshti_%26_shawl_-_India_21._Century_2023-04-13.jpg/960px-Yellow_cotton_Veshti_%26_shawl_-_India_21._Century_2023-04-13.jpg"),
            Map.entry("sweater", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/Grey_knitted_sweater.jpg/960px-Grey_knitted_sweater.jpg"),
            Map.entry("wool,socks", "https://upload.wikimedia.org/wikipedia/commons/1/18/Ladies_all-wool_socks_1904.jpg"),
            Map.entry("knitted,hat", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Beanie_hat_by_Polo_Ralph_Lauren.jpg/960px-Beanie_hat_by_Polo_Ralph_Lauren.jpg"),
            Map.entry("winter,gloves", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Swiss_Military_Wool_Mittens_%2815695449197%29.jpg/960px-Swiss_Military_Wool_Mittens_%2815695449197%29.jpg"),
            Map.entry("poncho", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/Poncho_MET_CI40.65.12_d1.jpg/960px-Poncho_MET_CI40.65.12_d1.jpg"),
            Map.entry("spoon", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Conrado_Barrio%2C_Carved_Wooden_Spoon%2C_1935-1942%2C_NGA_26926.jpg/960px-Conrado_Barrio%2C_Carved_Wooden_Spoon%2C_1935-1942%2C_NGA_26926.jpg"),
            Map.entry("box", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Carved_lacquer_wooden_box%2C_Ming_Dynasty3.jpg/960px-Carved_lacquer_wooden_box%2C_Ming_Dynasty3.jpg"),
            Map.entry("clock", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Wooden_wall_clock_with_massive_brass_weights_DSG5426-1_-_2020-04-25.jpg/960px-Wooden_wall_clock_with_massive_brass_weights_DSG5426-1_-_2020-04-25.jpg"),
            Map.entry("coaster", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Wooden_Tray.jpg/960px-Wooden_Tray.jpg"),
            Map.entry("tray", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Wooden_Tray.jpg/960px-Wooden_Tray.jpg"),
            Map.entry("bench", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Carved_wooden_bench_furniture_and_crafts_at_Heuan_Chan_heritage_house_in_Luang_Prabang_Laos.jpg/960px-Carved_wooden_bench_furniture_and_crafts_at_Heuan_Chan_heritage_house_in_Luang_Prabang_Laos.jpg"),
            Map.entry("frame", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Wooden_Picture_Frame_-_geograph.org.uk_-_4925889.jpg/960px-Wooden_Picture_Frame_-_geograph.org.uk_-_4925889.jpg"),
            Map.entry("choppingboard", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Wooden_cutting_board_2017.jpg/960px-Wooden_cutting_board_2017.jpg"),
            Map.entry("lamp", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Wooden_Antique_Table_Lamp.jpg/960px-Wooden_Antique_Table_Lamp.jpg"),
            Map.entry("shelf", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Illuminated_wooden_shelf_with_many_glass_jars_containing_cookies_for_sale_in_Tokyo.jpg/960px-Illuminated_wooden_shelf_with_many_glass_jars_containing_cookies_for_sale_in_Tokyo.jpg"),
            Map.entry("coatrack", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Illuminated_wooden_shelf_with_many_glass_jars_containing_cookies_for_sale_in_Tokyo.jpg/960px-Illuminated_wooden_shelf_with_many_glass_jars_containing_cookies_for_sale_in_Tokyo.jpg"),
            Map.entry("stand", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/DFC_4360_A_display_of_rugged_wristwatches_neatly_arranged_on_a_wooden_stand_ready_for_shoppers_to_try_on.jpg/960px-DFC_4360_A_display_of_rugged_wristwatches_neatly_arranged_on_a_wooden_stand_ready_for_shoppers_to_try_on.jpg"),
            Map.entry("stool", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/A_traditional_wooden_stool_in_Volta_region.jpg/960px-A_traditional_wooden_stool_in_Volta_region.jpg"),
            Map.entry("organizer", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Carved_lacquer_wooden_box%2C_Ming_Dynasty3.jpg/960px-Carved_lacquer_wooden_box%2C_Ming_Dynasty3.jpg")
    );
}
