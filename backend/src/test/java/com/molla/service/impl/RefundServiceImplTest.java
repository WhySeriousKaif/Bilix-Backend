package com.molla.service.impl;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.exceptions.BadRequestException;
import com.molla.model.Branch;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.Refund;
import com.molla.model.RefundItem;
import com.molla.model.User;
import com.molla.payload.dto.RefundDto;
import com.molla.payload.dto.RefundItemDto;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.ShiftReportRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock private RefundRepository refundRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserService userService;
    @Mock private ShiftReportRepository shiftReportRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AccessControlService accessControlService;
    @InjectMocks private RefundServiceImpl refundService;

    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        Branch branch = Branch.builder().id(10L).name("Main").build();
        User cashier = new User();
        cashier.setId(20L);
        cashier.setBranch(branch);

        product = new Product();
        product.setId(30L);
        product.setName("Coffee");
        product.setQuantity(5);

        order = Order.builder()
            .id(40L)
            .branch(branch)
            .cashier(cashier)
            .status(OrderStatus.COMPLETED)
            .paymentType(PaymentType.CASH)
            .build();
        orderItem = OrderItem.builder()
            .id(50L)
            .order(order)
            .product(product)
            .quantity(2)
            .price(10.0)
            .totalPrice(20.0)
            .build();
        order.setOrderItems(List.of(orderItem));

        when(userService.getCurrentUser()).thenReturn(cashier);
        when(orderRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(40L)).thenReturn(List.of());
    }

    @Test
    void fullRefundUsesOriginalPriceRestoresStockAndUpdatesOrder() throws Exception {
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefundDto request = requestForQuantity(2);
        request.setAmount(1.0); // Client totals are deliberately ignored.

        RefundDto result = refundService.createRefund(request);

        assertEquals(20.0, result.getAmount());
        assertEquals(7, product.getQuantity());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        assertEquals(2, result.getItems().get(0).getQuantity());
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsQuantityAlreadyRefunded() {
        Refund existing = Refund.builder()
            .amount(10.0)
            .items(List.of(RefundItem.builder().orderItem(orderItem).product(product).quantity(1).build()))
            .build();
        when(refundRepository.findByOrderId(40L)).thenReturn(List.of(existing));

        BadRequestException error = assertThrows(BadRequestException.class,
            () -> refundService.createRefund(requestForQuantity(2)));

        assertEquals("Only 1 unit(s) of Coffee remain refundable", error.getMessage());
        verify(refundRepository, never()).save(any(Refund.class));
    }

    private RefundDto requestForQuantity(int quantity) {
        return RefundDto.builder()
            .orderId(40L)
            .reason("Defective item")
            .paymentType(PaymentType.CASH)
            .items(List.of(RefundItemDto.builder().orderItemId(50L).quantity(quantity).build()))
            .build();
    }
}
