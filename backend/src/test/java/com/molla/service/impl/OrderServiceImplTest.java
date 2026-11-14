package com.molla.service.impl;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.exceptions.BadRequestException;
import com.molla.model.Branch;
import com.molla.model.Inventory;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.OrderDto;
import com.molla.payload.dto.OrderItemDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.CustomerRepository;
import com.molla.repository.InventoryRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Checkout safety tests.
 *
 * These cover the three properties the API relies on for money and stock:
 * a repeated request does not create a second order, the price always comes from the
 * database, and a sale cannot exceed the branch inventory.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private UserService userService;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccessControlService accessControlService;
    @Mock private RefundRepository refundRepository;
    @Mock private InventoryRepository inventoryRepository;
    @InjectMocks private OrderServiceImpl orderService;

    private Product product;
    private Branch branch;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Store store = new Store();
        store.setId(5L);

        branch = Branch.builder().id(7L).name("Main").store(store).build();

        User cashier = new User();
        cashier.setId(9L);
        cashier.setBranch(branch);

        product = new Product();
        product.setId(11L);
        product.setName("Headphones");
        product.setPrice(2499.0);
        product.setStore(store);

        inventory = new Inventory();
        inventory.setId(13L);
        inventory.setProduct(product);
        inventory.setBranch(branch);
        inventory.setQuantity(10);

        // Shared fixtures: not every test reaches all of these, so they are lenient.
        lenient().when(userService.getCurrentUser()).thenReturn(cashier);
        lenient().when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        lenient().when(inventoryRepository.findByProductIdAndBranchIdForUpdate(anyLong(), anyLong()))
                .thenReturn(Optional.of(inventory));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheOriginalOrder() throws Exception {
        Order existing = Order.builder()
                .id(42L)
                .branch(branch)
                .status(OrderStatus.COMPLETED)
                .paymentType(PaymentType.CASH)
                .build();
        when(orderRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        OrderDto result = orderService.createOrder(request(1, 1.0), "key-123");

        assertEquals(42L, result.getId());
        // A replay must not create a second order or deduct stock again.
        verifyNoInteractions(productRepository, inventoryRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutUsesTheStoredPriceAndIgnoresTheClientPrice() throws Exception {
        // The client claims the headphones cost 1.00; the database says 2,499.00.
        OrderDto result = orderService.createOrder(request(2, 1.0), "key-456");

        assertEquals(2, result.getOrderItems().get(0).getQuantity());
        assertEquals(2499.0, result.getOrderItems().get(0).getPrice());
        assertEquals(4998.0, result.getTotalAmount());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        OrderItem saved = captor.getValue().getOrderItems().get(0);
        assertEquals(2499.0, saved.getPrice());
        assertEquals(4998.0, saved.getTotalPrice());
        // Stock moved from 10 to 8.
        assertEquals(8, inventory.getQuantity());
    }

    @Test
    void checkoutRejectsQuantityAboveBranchStock() {
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request(11, 2499.0), "key-789"));

        assertEquals("Not enough stock for Headphones (available: 10, requested: 11)",
                error.getMessage());
        assertEquals(10, inventory.getQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutRequiresAnIdempotencyKey() {
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request(1, 2499.0), "  "));

        assertEquals("Idempotency-Key header is required", error.getMessage());
        assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request(1, 2499.0), null));
    }

    private OrderDto request(int quantity, double clientPrice) {
        return OrderDto.builder()
                .paymentType(PaymentType.CASH)
                .orderItems(List.of(OrderItemDto.builder()
                        .productId(11L)
                        .quantity(quantity)
                        .price(clientPrice)
                        .build()))
                .build();
    }
}
