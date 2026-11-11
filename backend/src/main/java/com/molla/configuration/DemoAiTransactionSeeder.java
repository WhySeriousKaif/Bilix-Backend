package com.molla.configuration;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.User;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.demo-ai-data.enabled",
        havingValue = "true"
)
public class DemoAiTransactionSeeder {

    private static final int TRANSACTION_COUNT = 500;
    private static final String KEY_PREFIX = "ai-demo-v1-";

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDemoTransactions() {

        User cashier = userRepository.findByEmail(
                "cashier@molla.com"
        );

        if (cashier == null || cashier.getBranch() == null) {
            throw new IllegalStateException(
                    "Demo cashier or branch is missing"
            );
        }

        Product headphones = requiredProduct(
                "ELEC-HD-001"
        );

        Product television = requiredProduct(
                "ELEC-TV-002"
        );

        Product coffee = requiredProduct(
                "BEV-CF-003"
        );

        Product jacket = requiredProduct(
                "FASH-JK-004"
        );

        Product flask = requiredProduct(
                "HOME-FL-005"
        );

        Product chair = requiredProduct(
                "FURN-CH-006"
        );

        Product keyboard = requiredProduct(
                "ELEC-KB-007"
        );

        Product fitnessTracker = requiredProduct(
                "ELEC-SW-008"
        );

        Product chocolate = requiredProduct(
                "BEV-CH-009"
        );

        Product duffleBag = requiredProduct(
                "FASH-BG-010"
        );

        List<Product> allProducts = List.of(
                headphones,
                television,
                coffee,
                jacket,
                flask,
                chair,
                keyboard,
                fitnessTracker,
                chocolate,
                duffleBag
        );

        Random random = new Random(42);
        int created = 0;
        int skipped = 0;

        for (int index = 1;
             index <= TRANSACTION_COUNT;
             index++) {

            String idempotencyKey = KEY_PREFIX + index;

            if (orderRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .isPresent()) {

                skipped++;
                continue;
            }

            List<Product> basket = createBasket(
                    random,
                    coffee,
                    chocolate,
                    keyboard,
                    headphones,
                    fitnessTracker,
                    flask,
                    jacket,
                    duffleBag,
                    chair,
                    television,
                    allProducts
            );

            LocalDateTime occurredAt =
                    randomDateInLastThirtyDays(random);

            Order order = createOrder(
                    cashier,
                    basket,
                    occurredAt,
                    idempotencyKey,
                    random
            );

            orderRepository.save(order);
            created++;
        }

        log.info(
                "AI demo data completed: created={}, skipped={}",
                created,
                skipped
        );
    }

    private Product requiredProduct(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Required demo product is missing: "
                                        + sku
                        )
                );
    }

    private List<Product> createBasket(
            Random random,
            Product coffee,
            Product chocolate,
            Product keyboard,
            Product headphones,
            Product fitnessTracker,
            Product flask,
            Product jacket,
            Product duffleBag,
            Product chair,
            Product television,
            List<Product> allProducts
    ) {
        int choice = random.nextInt(100);

        List<Product> basket = new ArrayList<>();

        if (choice < 30) {
            // Strongest relationship
            basket.add(coffee);
            basket.add(chocolate);

            if (random.nextInt(100) < 25) {
                basket.add(flask);
            }

        } else if (choice < 52) {
            basket.add(keyboard);
            basket.add(headphones);

            if (random.nextInt(100) < 20) {
                basket.add(chair);
            }

        } else if (choice < 67) {
            basket.add(fitnessTracker);
            basket.add(flask);

        } else if (choice < 79) {
            basket.add(jacket);
            basket.add(duffleBag);

        } else if (choice < 89) {
            basket.add(television);
            basket.add(headphones);

        } else {
            // Noise/single-product baskets are important.
            basket.add(
                    allProducts.get(
                            random.nextInt(allProducts.size())
                    )
            );
        }

        return basket;
    }

    private LocalDateTime randomDateInLastThirtyDays(
            Random random
    ) {
        LocalDateTime now = LocalDateTime.now();

        int daysAgo = random.nextInt(30);
        int hour = 9 + random.nextInt(12);
        int minute = random.nextInt(60);

        return now
                .minusDays(daysAgo)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);
    }

    private Order createOrder(
            User cashier,
            List<Product> products,
            LocalDateTime occurredAt,
            String idempotencyKey,
            Random random
    ) {
        Order order = Order.builder()
                .branch(cashier.getBranch())
                .cashier(cashier)
                .customer(null)
                .createdAt(occurredAt)
                .paymentType(randomPaymentType(random))
                .status(OrderStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .orderItems(new ArrayList<>())
                .build();

        double totalAmount = 0.0;

        for (Product product : products) {
            int quantity = random.nextInt(100) < 90
                    ? 1
                    : 2;

            double unitPrice = resolvePrice(product);
            double lineTotal = unitPrice * quantity;

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .price(unitPrice)
                    .totalPrice(lineTotal)
                    .build();

            order.getOrderItems().add(item);
            totalAmount += lineTotal;
        }

        order.setTotalAmount(totalAmount);

        return order;
    }

    private double resolvePrice(Product product) {
        if (product.getPrice() != null) {
            return product.getPrice();
        }

        if (product.getSellingPrice() != null) {
            return product.getSellingPrice();
        }

        if (product.getMrp() != null) {
            return product.getMrp();
        }

        throw new IllegalStateException(
                "Product has no price: " + product.getSku()
        );
    }

    private PaymentType randomPaymentType(Random random) {
        PaymentType[] values = PaymentType.values();

        return values[random.nextInt(values.length)];
    }
}