package com.molla.configuration;

import com.molla.model.Category;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.repository.CategoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.demo-forecast-products.enabled",
        havingValue = "true"
)
public class DemoForecastProductSeeder {

    private static final String DEMO_CASHIER_EMAIL =
            "cashier@molla.com";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Order(10)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedForecastProducts() {
        User cashier = userRepository.findByEmail(
                DEMO_CASHIER_EMAIL
        );

        if (cashier == null
                || cashier.getBranch() == null
                || cashier.getBranch().getStore() == null) {
            throw new IllegalStateException(
                    "Demo cashier, branch, or store is missing"
            );
        }

        Store store = cashier.getBranch().getStore();
        Map<String, Category> categories = new HashMap<>();
        int created = 0;
        int skipped = 0;

        for (ProductSeed seed : PRODUCT_SEEDS) {
            if (productRepository.findBySku(seed.sku())
                    .isPresent()) {
                skipped++;
                continue;
            }

            Category category = categories.computeIfAbsent(
                    seed.category(),
                    name -> findOrCreateCategory(store, name)
            );

            Product product = new Product();
            product.setSku(seed.sku());
            product.setName(seed.name());
            product.setDescription(seed.description());
            product.setBrand(seed.brand());
            product.setMrp(seed.mrp());
            product.setSellingPrice(seed.sellingPrice());
            product.setPrice(seed.sellingPrice());
            product.setQuantity(seed.initialStock());
            product.setImage(null);
            product.setStore(store);
            product.setCategory(category);

            productRepository.save(product);
            created++;
        }

        log.info(
                "Forecast product catalogue completed: "
                        + "created={}, skipped={}, storeId={}",
                created,
                skipped,
                store.getId()
        );
    }

    private Category findOrCreateCategory(
            Store store,
            String categoryName
    ) {
        return categoryRepository
                .findFirstByStoreIdAndNameIgnoreCase(
                        store.getId(),
                        categoryName
                )
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(categoryName);
                    category.setDescription(
                            "Demo forecasting category: "
                                    + categoryName
                    );
                    category.setStore(store);
                    return categoryRepository.save(category);
                });
    }

    private record ProductSeed(
            String sku,
            String name,
            String description,
            String category,
            String brand,
            double mrp,
            double sellingPrice,
            int initialStock
    ) {
    }

    private static ProductSeed product(
            String sku,
            String name,
            String category,
            String brand,
            double mrp,
            double sellingPrice,
            int initialStock
    ) {
        return new ProductSeed(
                sku,
                name,
                name + " for realistic retail demand forecasting",
                category,
                brand,
                mrp,
                sellingPrice,
                initialStock
        );
    }

    private static final List<ProductSeed> PRODUCT_SEEDS = List.of(
            product("FC-BAK-001", "Classic White Bread 400g", "Bakery", "DailyBake", 50, 45, 180),
            product("FC-BAK-002", "Whole Wheat Bread 400g", "Bakery", "DailyBake", 60, 54, 160),
            product("FC-BAK-003", "Multigrain Bread 400g", "Bakery", "HealthyLoaf", 75, 68, 120),
            product("FC-BAK-004", "Burger Buns Pack of 4", "Bakery", "DailyBake", 55, 49, 110),
            product("FC-BAK-005", "Butter Croissant Pack of 2", "Bakery", "BakeHouse", 90, 82, 90),

            product("FC-DAI-001", "Full Cream Milk 1L", "Dairy & Eggs", "FarmFresh", 72, 68, 260),
            product("FC-DAI-002", "Salted Butter 500g", "Dairy & Eggs", "FarmFresh", 290, 275, 130),
            product("FC-DAI-003", "Plain Curd 500g", "Dairy & Eggs", "FarmFresh", 70, 65, 180),
            product("FC-DAI-004", "Fresh Paneer 200g", "Dairy & Eggs", "FarmFresh", 110, 102, 120),
            product("FC-DAI-005", "Cheese Slices Pack of 10", "Dairy & Eggs", "MilkyWay", 165, 155, 100),
            product("FC-DAI-006", "Farm Eggs Pack of 12", "Dairy & Eggs", "GoldenYolk", 110, 99, 200),

            product("FC-SPR-001", "Strawberry Jam 500g", "Spreads & Sauces", "BerryBest", 210, 189, 100),
            product("FC-SPR-002", "Mixed Fruit Jam 500g", "Spreads & Sauces", "BerryBest", 195, 178, 110),
            product("FC-SPR-003", "Crunchy Peanut Butter 340g", "Spreads & Sauces", "NutriSpread", 260, 235, 90),
            product("FC-SPR-004", "Natural Honey 500g", "Spreads & Sauces", "NatureDrop", 320, 295, 80),
            product("FC-SPR-005", "Tomato Ketchup 500g", "Spreads & Sauces", "KitchenMate", 145, 130, 140),
            product("FC-SPR-006", "Classic Mayonnaise 250g", "Spreads & Sauces", "KitchenMate", 120, 108, 100),

            product("FC-STP-001", "Basmati Rice 5kg", "Staples", "GrainGold", 720, 665, 100),
            product("FC-STP-002", "Whole Wheat Atta 5kg", "Staples", "GrainGold", 330, 305, 130),
            product("FC-STP-003", "Toor Dal 1kg", "Staples", "HomeHarvest", 190, 175, 140),
            product("FC-STP-004", "Chana Dal 1kg", "Staples", "HomeHarvest", 150, 138, 120),
            product("FC-STP-005", "Refined Sunflower Oil 1L", "Staples", "PureDrop", 165, 153, 170),
            product("FC-STP-006", "Crystal Sugar 1kg", "Staples", "HomeHarvest", 60, 55, 200),
            product("FC-STP-007", "Iodized Salt 1kg", "Staples", "DailySalt", 28, 25, 220),

            product("FC-BEV-001", "Assam Tea 500g", "Beverages", "MorningLeaf", 310, 285, 100),
            product("FC-BEV-002", "Instant Coffee 100g", "Beverages", "WakeUp", 240, 220, 90),
            product("FC-BEV-003", "Orange Juice 1L", "Beverages", "FruitFlow", 135, 125, 130),
            product("FC-BEV-004", "Mango Drink 1L", "Beverages", "FruitFlow", 110, 99, 140),
            product("FC-BEV-005", "Sparkling Cola 750ml", "Beverages", "FizzUp", 45, 42, 220),
            product("FC-BEV-006", "Packaged Drinking Water 1L", "Beverages", "ClearSpring", 25, 22, 300),

            product("FC-SNK-001", "Classic Salted Potato Chips 90g", "Snacks", "CrunchTime", 50, 45, 220),
            product("FC-SNK-002", "Masala Potato Chips 90g", "Snacks", "CrunchTime", 50, 45, 210),
            product("FC-SNK-003", "Marie Biscuits 250g", "Snacks", "TeaBuddy", 45, 41, 230),
            product("FC-SNK-004", "Chocolate Cream Biscuits 150g", "Snacks", "TeaBuddy", 40, 36, 180),
            product("FC-SNK-005", "Spicy Namkeen 400g", "Snacks", "SnackStreet", 120, 108, 150),
            product("FC-SNK-006", "Instant Noodles Pack of 4", "Snacks", "QuickBite", 80, 74, 190),
            product("FC-SNK-007", "Breakfast Corn Flakes 475g", "Snacks", "MorningBowl", 210, 195, 100),

            product("FC-PER-001", "Bath Soap Pack of 4", "Personal Care", "FreshGlow", 180, 165, 130),
            product("FC-PER-002", "Daily Shampoo 340ml", "Personal Care", "FreshGlow", 260, 240, 100),
            product("FC-PER-003", "Herbal Toothpaste 200g", "Personal Care", "SmileCare", 145, 132, 150),
            product("FC-PER-004", "Hand Sanitizer 500ml", "Personal Care", "SafeHands", 210, 185, 90),
            product("FC-PER-005", "Facial Tissue Box 200 Pulls", "Personal Care", "SoftTouch", 110, 99, 120),

            product("FC-HOU-001", "Laundry Detergent 2kg", "Household Care", "CleanHome", 340, 315, 110),
            product("FC-HOU-002", "Dishwash Liquid 750ml", "Household Care", "CleanHome", 180, 165, 130),
            product("FC-HOU-003", "Floor Cleaner 1L", "Household Care", "CleanHome", 220, 199, 100),
            product("FC-HOU-004", "Garbage Bags Medium Pack of 30", "Household Care", "HomeEase", 160, 145, 90),

            product("FC-FRE-001", "Bananas 1kg", "Fresh Produce", "FreshBasket", 70, 62, 180),
            product("FC-FRE-002", "Apples 1kg", "Fresh Produce", "FreshBasket", 220, 199, 120),
            product("FC-FRE-003", "Tomatoes 1kg", "Fresh Produce", "FreshBasket", 65, 58, 180),
            product("FC-FRE-004", "Potatoes 1kg", "Fresh Produce", "FreshBasket", 55, 49, 220)
    );
}
