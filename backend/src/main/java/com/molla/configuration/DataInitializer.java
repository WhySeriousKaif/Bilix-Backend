package com.molla.configuration;

import com.molla.domain.StoreStatus;
import com.molla.domain.UserRole;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.StoreContact;
import com.molla.model.SubscriptionPlan;
import com.molla.model.User;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.SubscriptionPlanRepository;
import com.molla.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 🚀 Data Initializer - Automatically seeds default role credentials, demo store, and branch on startup
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

            // 6. Cashier (ROLE_CASHIER)
            User cashier = userRepository.findByEmail("cashier@molla.com");
            if (cashier == null) {
                cashier = new User();
                cashier.setFullName("Cashier One");
                cashier.setEmail("cashier@molla.com");
                cashier.setPassword(passwordEncoder.encode("Cashier123@"));
                cashier.setRole(UserRole.ROLE_CASHIER);
                cashier.setStore(store);
                cashier.setBranch(branch);
                cashier.setCreatedAt(LocalDateTime.now());
                cashier.setUpdatedAt(LocalDateTime.now());
                userRepository.save(cashier);
                logger.info("✅ Created default Cashier: cashier@molla.com");
            } else if (cashier.getBranch() == null) {
                cashier.setStore(store);
                cashier.setBranch(branch);
                userRepository.save(cashier);
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
        };
    }
}
