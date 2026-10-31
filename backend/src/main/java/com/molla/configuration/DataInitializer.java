package com.molla.configuration;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.domain.StoreStatus;
import com.molla.domain.UserRole;
import com.molla.model.*;
import com.molla.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 🚀 Data Initializer - Automatically seeds default role credentials, demo store, branch, products, customers, and orders on startup
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            StoreRepository storeRepository,
            BranchRepository branchRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            RefundRepository refundRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Super Admin (ROLE_ADMIN)
            User admin = userRepository.findByEmail("kaiftruth101@gmail.com");
            if (admin == null) {
                admin = new User();
                admin.setFullName("Kaif");
                admin.setEmail("kaiftruth101@gmail.com");
                admin.setPassword(passwordEncoder.encode("Kaif0786@"));
                admin.setRole(UserRole.ROLE_ADMIN);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                admin = userRepository.save(admin);
                logger.info("✅ Created default Super Admin: kaiftruth101@gmail.com");
            }

            // 2. Store Admin (ROLE_STORE_ADMIN)
            User storeAdmin = userRepository.findByEmail("storeadmin@molla.com");
            if (storeAdmin == null) {
                storeAdmin = new User();
                storeAdmin.setFullName("Store Admin");
                storeAdmin.setEmail("storeadmin@molla.com");
                storeAdmin.setPassword(passwordEncoder.encode("StoreAdmin123@"));
                storeAdmin.setRole(UserRole.ROLE_STORE_ADMIN);
                storeAdmin.setCreatedAt(LocalDateTime.now());
                storeAdmin.setUpdatedAt(LocalDateTime.now());
                storeAdmin = userRepository.save(storeAdmin);
                logger.info("✅ Created default Store Admin: storeadmin@molla.com");
            }

            // 3. Demo Store
            Store store = storeRepository.findAll().stream().findFirst().orElse(null);
            if (store == null) {
                store = new Store();
                store.setBrand("Molla Main Store");
                store.setDescription("Primary Retail & POS Outlet");
                store.setStoreType("Supermarket");
                store.setStoreStatus(StoreStatus.ACTIVE);
                store.setStoreAdmin(storeAdmin);

                StoreContact contact = new StoreContact();
                contact.setAddress("123 Commerce Way, Tech City");
                contact.setEmail("contact@mollapos.com");
                contact.setPhone("9876543210");
                store.setContact(contact);

                store = storeRepository.save(store);
                logger.info("✅ Created default Store: Molla Main Store");
            }

            // Link store to storeAdmin
            if (storeAdmin.getStore() == null) {
                storeAdmin.setStore(store);
                storeAdmin = userRepository.save(storeAdmin);
            }

            // 4. Branch Manager (ROLE_BRANCH_MANAGER)
            User manager = userRepository.findByEmail("manager@molla.com");
            if (manager == null) {
                manager = new User();
                manager.setFullName("Branch Manager");
                manager.setEmail("manager@molla.com");
                manager.setPassword(passwordEncoder.encode("Manager123@"));
                manager.setRole(UserRole.ROLE_BRANCH_MANAGER);
                manager.setStore(store);
                manager.setCreatedAt(LocalDateTime.now());
                manager.setUpdatedAt(LocalDateTime.now());
                manager = userRepository.save(manager);
                logger.info("✅ Created default Branch Manager: manager@molla.com");
            }

            // 5. Demo Branch
            Branch branch = branchRepository.findAll().stream().findFirst().orElse(null);
            if (branch == null) {
                branch = new Branch();
                branch.setName("Main Branch");
                branch.setAddress("123 Commerce Way, Tech City");
                branch.setPhone("9876543210");
                branch.setEmail("mainbranch@mollapos.com");
                branch.setWorkingDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"));
                branch.setOpenTime(LocalTime.of(9, 0));
                branch.setCloseTime(LocalTime.of(21, 0));
                branch.setStore(store);
                branch.setManager(manager);
                branch = branchRepository.save(branch);
                logger.info("✅ Created default Branch: Main Branch");
            }

            // Link branch to manager
            if (manager.getBranch() == null) {
                manager.setBranch(branch);
                manager = userRepository.save(manager);
            }

            // 6. Cashiers (ROLE_CASHIER)
            User cashier = userRepository.findByEmail("cashier@molla.com");
            if (cashier == null) {
                cashier = new User();
                cashier.setFullName("Alex Johnson");
                cashier.setEmail("cashier@molla.com");
                cashier.setPassword(passwordEncoder.encode("Cashier123@"));
                cashier.setRole(UserRole.ROLE_CASHIER);
                cashier.setStore(store);
                cashier.setBranch(branch);
                cashier.setCreatedAt(LocalDateTime.now());
                cashier.setUpdatedAt(LocalDateTime.now());
                cashier = userRepository.save(cashier);
                logger.info("✅ Created default Cashier: Alex Johnson (cashier@molla.com)");
            } else if (cashier.getBranch() == null) {
                cashier.setStore(store);
                cashier.setBranch(branch);
                cashier = userRepository.save(cashier);
            }

            User cashier2 = userRepository.findByEmail("cashier2@molla.com");
            if (cashier2 == null) {
                cashier2 = new User();
                cashier2.setFullName("Sarah Connor");
                cashier2.setEmail("cashier2@molla.com");
                cashier2.setPassword(passwordEncoder.encode("Cashier123@"));
                cashier2.setRole(UserRole.ROLE_CASHIER);
                cashier2.setStore(store);
                cashier2.setBranch(branch);
                cashier2.setCreatedAt(LocalDateTime.now());
                cashier2.setUpdatedAt(LocalDateTime.now());
                userRepository.save(cashier2);
                logger.info("✅ Created Cashier 2: Sarah Connor");
            }

            // 7. Default subscription plans
            if (subscriptionPlanRepository.count() == 0) {
                subscriptionPlanRepository.save(SubscriptionPlan.builder()
                        .name("Basic Plan")
                        .price(1299.0)
                        .maxBranches(10)
                        .maxUsers(50)
                        .maxProducts(1000)
                        .features("API integrations, Advanced reporting, Email support")
                        .active(true)
                        .build());
                subscriptionPlanRepository.save(SubscriptionPlan.builder()
                        .name("Pro Plan")
                        .price(2999.0)
                        .maxBranches(100)
                        .maxUsers(500)
                        .maxProducts(9000)
                        .features("API integrations, Advanced reporting, Shift management, Priority support")
                        .active(true)
                        .build());
                subscriptionPlanRepository.save(SubscriptionPlan.builder()
                        .name("Advance Plan")
                        .price(4999.0)
                        .maxBranches(400)
                        .maxUsers(5000)
                        .maxProducts(50000)
                        .features("All Pro features, Custom integrations, Dedicated support, Custom branding")
                        .active(true)
                        .build());
                logger.info("✅ Created default subscription plans");
            }

            // 8. Categories
            Category catElectronics = categoryRepository.findAll().stream().filter(c -> "Electronics".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            if (catElectronics == null) {
                catElectronics = new Category();
                catElectronics.setName("Electronics");
                catElectronics.setStore(store);
                catElectronics = categoryRepository.save(catElectronics);
            }

            Category catBeverages = categoryRepository.findAll().stream().filter(c -> "Beverages & Snacks".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            if (catBeverages == null) {
                catBeverages = new Category();
                catBeverages.setName("Beverages & Snacks");
                catBeverages.setStore(store);
                catBeverages = categoryRepository.save(catBeverages);
            }

            Category catFashion = categoryRepository.findAll().stream().filter(c -> "Fashion & Apparel".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            if (catFashion == null) {
                catFashion = new Category();
                catFashion.setName("Fashion & Apparel");
                catFashion.setStore(store);
                catFashion = categoryRepository.save(catFashion);
            }

            Category catHome = categoryRepository.findAll().stream().filter(c -> "Home & Living".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            if (catHome == null) {
                catHome = new Category();
                catHome.setName("Home & Living");
                catHome.setStore(store);
                catHome = categoryRepository.save(catHome);
            }

            // 9. Products & Inventory
            List<Product> seededProducts = new ArrayList<>();
            if (productRepository.count() < 5) {
                Product p1 = new Product();
                p1.setName("Wireless Noise-Canceling Headphones");
                p1.setDescription("Premium over-ear Bluetooth headphones with active noise cancellation");
                p1.setSku("ELEC-HD-001");
                p1.setMrp(5999.0);
                p1.setSellingPrice(4999.0);
                p1.setPrice(4999.0);
                p1.setQuantity(45);
                p1.setBrand("SoundPro");
                p1.setImage("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60");
                p1.setStore(store);
                p1.setCategory(catElectronics);
                p1 = productRepository.save(p1);
                seededProducts.add(p1);

                Product p2 = new Product();
                p2.setName("Smart HD LED Television 43-inch");
                p2.setDescription("4K Ultra HD Smart TV with Voice Assistant built-in");
                p2.setSku("ELEC-TV-002");
                p2.setMrp(29999.0);
                p2.setSellingPrice(24999.0);
                p2.setPrice(24999.0);
                p2.setQuantity(12);
                p2.setBrand("VisionPlus");
                p2.setImage("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=500&auto=format&fit=crop&q=60");
                p2.setStore(store);
                p2.setCategory(catElectronics);
                p2 = productRepository.save(p2);
                seededProducts.add(p2);

                Product p3 = new Product();
                p3.setName("Organic Arabica Coffee Beans 500g");
                p3.setDescription("Single-origin dark roast freshly ground coffee beans");
                p3.setSku("BEV-CF-003");
                p3.setMrp(899.0);
                p3.setSellingPrice(749.0);
                p3.setPrice(749.0);
                p3.setQuantity(120);
                p3.setBrand("BrewCraft");
                p3.setImage("https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500&auto=format&fit=crop&q=60");
                p3.setStore(store);
                p3.setCategory(catBeverages);
                p3 = productRepository.save(p3);
                seededProducts.add(p3);

                Product p4 = new Product();
                p4.setName("Premium Denim Jacket");
                p4.setDescription("Classic fit vintage washed indigo cotton denim jacket");
                p4.setSku("FASH-JK-004");
                p4.setMrp(3499.0);
                p4.setSellingPrice(2499.0);
                p4.setPrice(2499.0);
                p4.setQuantity(30);
                p4.setBrand("UrbanOutfit");
                p4.setImage("https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=500&auto=format&fit=crop&q=60");
                p4.setStore(store);
                p4.setCategory(catFashion);
                p4 = productRepository.save(p4);
                seededProducts.add(p4);

                Product p5 = new Product();
                p5.setName("Stainless Steel Water Flask 1L");
                p5.setDescription("Vacuum insulated thermal bottle keeps drinks cold for 24h");
                p5.setSku("HOME-FL-005");
                p5.setMrp(1199.0);
                p5.setSellingPrice(899.0);
                p5.setPrice(899.0);
                p5.setQuantity(85);
                p5.setBrand("HydroGear");
                p5.setImage("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=500&auto=format&fit=crop&q=60");
                p5.setStore(store);
                p5.setCategory(catHome);
                p5 = productRepository.save(p5);
                seededProducts.add(p5);

                Product p6 = new Product();
                p6.setName("Ergonomic Mesh Office Chair");
                p6.setDescription("High-back breathable mesh chair with lumbar support and adjustable armrests");
                p6.setSku("FURN-CH-006");
                p6.setMrp(12999.0);
                p6.setSellingPrice(9999.0);
                p6.setPrice(9999.0);
                p6.setQuantity(20);
                p6.setBrand("ErgoFlex");
                p6.setImage("https://images.unsplash.com/photo-1580481072645-022f9a6d120d?w=500&auto=format&fit=crop&q=60");
                p6.setStore(store);
                p6.setCategory(catHome);
                p6 = productRepository.save(p6);
                seededProducts.add(p6);

                Product p7 = new Product();
                p7.setName("RGB Mechanical Gaming Keyboard");
                p7.setDescription("Tactile mechanical switches with per-key RGB backlighting");
                p7.setSku("ELEC-KB-007");
                p7.setMrp(4499.0);
                p7.setSellingPrice(3499.0);
                p7.setPrice(3499.0);
                p7.setQuantity(50);
                p7.setBrand("CyberKey");
                p7.setImage("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop&q=60");
                p7.setStore(store);
                p7.setCategory(catElectronics);
                p7 = productRepository.save(p7);
                seededProducts.add(p7);

                Product p8 = new Product();
                p8.setName("Smart Fitness Tracker Watch");
                p8.setDescription("Heart rate monitoring, SPO2 sensor, GPS & 14-day battery life");
                p8.setSku("ELEC-SW-008");
                p8.setMrp(6999.0);
                p8.setSellingPrice(5499.0);
                p8.setPrice(5499.0);
                p8.setQuantity(40);
                p8.setBrand("PulseFit");
                p8.setImage("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60");
                p8.setStore(store);
                p8.setCategory(catElectronics);
                p8 = productRepository.save(p8);
                seededProducts.add(p8);

                Product p9 = new Product();
                p9.setName("Gourmet Dark Chocolate Truffles 250g");
                p9.setDescription("Artisanal 70% dark Belgian cocoa truffles infused with sea salt");
                p9.setSku("BEV-CH-009");
                p9.setMrp(649.0);
                p9.setSellingPrice(499.0);
                p9.setPrice(499.0);
                p9.setQuantity(95);
                p9.setBrand("ChocoLuxe");
                p9.setImage("https://images.unsplash.com/photo-1549007994-cb92caebd54b?w=500&auto=format&fit=crop&q=60");
                p9.setStore(store);
                p9.setCategory(catBeverages);
                p9 = productRepository.save(p9);
                seededProducts.add(p9);

                Product p10 = new Product();
                p10.setName("Full Grain Leather Travel Duffle Bag");
                p10.setDescription("Handcrafted genuine leather weekender duffle bag with shoe compartment");
                p10.setSku("FASH-BG-010");
                p10.setMrp(8999.0);
                p10.setSellingPrice(6999.0);
                p10.setPrice(6999.0);
                p10.setQuantity(15);
                p10.setBrand("HeritageLeather");
                p10.setImage("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500&auto=format&fit=crop&q=60");
                p10.setStore(store);
                p10.setCategory(catFashion);
                p10 = productRepository.save(p10);
                seededProducts.add(p10);

                logger.info("✅ Seeded 10 realistic products with AI Unsplash URLs");
            } else {
                seededProducts = productRepository.findAll();
            }

            // Seed Branch Inventory
            final Branch currentBranch = branch;
            for (Product p : seededProducts) {
                boolean invExists = inventoryRepository.findAll().stream()
                        .anyMatch(i -> i.getBranch() != null && i.getBranch().getId().equals(currentBranch.getId())
                                && i.getProduct() != null && i.getProduct().getId().equals(p.getId()));
                if (!invExists) {
                    Inventory inv = Inventory.builder()
                            .branch(currentBranch)
                            .product(p)
                            .quantity(p.getQuantity() != null ? p.getQuantity() : 50)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    inventoryRepository.save(inv);
                }
            }

            // 10. Customers
            Customer cust1 = customerRepository.findAll().stream().filter(c -> "rahul.sharma@example.com".equalsIgnoreCase(c.getEmail())).findFirst().orElse(null);
            if (cust1 == null) {
                cust1 = new Customer();
                cust1.setName("Rahul Sharma");
                cust1.setEmail("rahul.sharma@example.com");
                cust1.setPhone("+91 9876512340");
                cust1.setCreatedAt(LocalDateTime.now().minusDays(10));
                cust1 = customerRepository.save(cust1);
            }

            Customer cust2 = customerRepository.findAll().stream().filter(c -> "priya.patel@example.com".equalsIgnoreCase(c.getEmail())).findFirst().orElse(null);
            if (cust2 == null) {
                cust2 = new Customer();
                cust2.setName("Priya Patel");
                cust2.setEmail("priya.patel@example.com");
                cust2.setPhone("+91 9876523451");
                cust2.setCreatedAt(LocalDateTime.now().minusDays(8));
                cust2 = customerRepository.save(cust2);
            }

            Customer cust3 = customerRepository.findAll().stream().filter(c -> "amit.verma@example.com".equalsIgnoreCase(c.getEmail())).findFirst().orElse(null);
            if (cust3 == null) {
                cust3 = new Customer();
                cust3.setName("Amit Verma");
                cust3.setEmail("amit.verma@example.com");
                cust3.setPhone("+91 9876534562");
                cust3.setCreatedAt(LocalDateTime.now().minusDays(5));
                cust3 = customerRepository.save(cust3);
            }

            // 11. Past 7-Day Real Orders & OrderItems for PostgreSQL
            if (orderRepository.count() == 0 && !seededProducts.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();

                // Today Order 1
                Product prod1 = seededProducts.get(0);
                Product prod3 = seededProducts.size() > 2 ? seededProducts.get(2) : prod1;

                OrderItem item1 = OrderItem.builder().product(prod1).quantity(1).price(prod1.getSellingPrice()).totalPrice(prod1.getSellingPrice()).build();
                OrderItem item2 = OrderItem.builder().product(prod3).quantity(2).price(prod3.getSellingPrice()).totalPrice(prod3.getSellingPrice() * 2).build();

                double total1 = item1.getTotalPrice() + item2.getTotalPrice();
                Order o1 = Order.builder()
                        .branch(branch)
                        .cashier(cashier)
                        .customer(cust1)
                        .totalAmount(total1)
                        .paymentType(PaymentType.CARD)
                        .status(OrderStatus.COMPLETED)
                        .createdAt(now.minusHours(2))
                        .orderItems(new ArrayList<>())
                        .build();
                item1.setOrder(o1);
                item2.setOrder(o1);
                o1.getOrderItems().add(item1);
                o1.getOrderItems().add(item2);
                o1 = orderRepository.save(o1);

                // Today Order 2
                Product prod5 = seededProducts.size() > 4 ? seededProducts.get(4) : prod1;
                OrderItem item3 = OrderItem.builder().product(prod5).quantity(1).price(prod5.getSellingPrice()).totalPrice(prod5.getSellingPrice()).build();
                Order o2 = Order.builder()
                        .branch(branch)
                        .cashier(cashier)
                        .customer(cust2)
                        .totalAmount(prod5.getSellingPrice())
                        .paymentType(PaymentType.UPI)
                        .status(OrderStatus.COMPLETED)
                        .createdAt(now.minusHours(4))
                        .orderItems(new ArrayList<>())
                        .build();
                item3.setOrder(o2);
                o2.getOrderItems().add(item3);
                o2 = orderRepository.save(o2);

                // Yesterday Order
                Product prod4 = seededProducts.size() > 3 ? seededProducts.get(3) : prod1;
                OrderItem item4 = OrderItem.builder().product(prod4).quantity(1).price(prod4.getSellingPrice()).totalPrice(prod4.getSellingPrice()).build();
                Order o3 = Order.builder()
                        .branch(branch)
                        .cashier(cashier)
                        .customer(cust3)
                        .totalAmount(prod4.getSellingPrice())
                        .paymentType(PaymentType.CASH)
                        .status(OrderStatus.COMPLETED)
                        .createdAt(now.minusDays(1).minusHours(3))
                        .orderItems(new ArrayList<>())
                        .build();
                item4.setOrder(o3);
                o3.getOrderItems().add(item4);
                o3 = orderRepository.save(o3);

                // 2 Days Ago Order
                Product prod2 = seededProducts.size() > 1 ? seededProducts.get(1) : prod1;
                OrderItem item5 = OrderItem.builder().product(prod2).quantity(1).price(prod2.getSellingPrice()).totalPrice(prod2.getSellingPrice()).build();
                Order o4 = Order.builder()
                        .branch(branch)
                        .cashier(cashier)
                        .customer(cust1)
                        .totalAmount(prod2.getSellingPrice())
                        .paymentType(PaymentType.CARD)
                        .status(OrderStatus.COMPLETED)
                        .createdAt(now.minusDays(2).minusHours(5))
                        .orderItems(new ArrayList<>())
                        .build();
                item5.setOrder(o4);
                o4.getOrderItems().add(item5);
                o4 = orderRepository.save(o4);

                // Refund record linked to o3
                Refund ref = Refund.builder()
                        .order(o3)
                        .amount(500.0)
                        .reason("Partial item exchange request")
                        .cashier(cashier)
                        .branch(branch)
                        .paymentType(PaymentType.CASH)
                        .createdAt(now.minusHours(1))
                        .build();
                refundRepository.save(ref);

                logger.info("✅ Seeded real orders and refunds for past 7 days into PostgreSQL");
            }
        };
    }
}
