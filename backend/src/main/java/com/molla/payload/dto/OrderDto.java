package com.molla.payload.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.molla.model.Customer;
import com.molla.domain.PaymentType;
import com.molla.domain.OrderStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private Long id;
    private Double totalAmount;
    private Long branchId;
    private Long cashierId;
    private Long customerId;
    private LocalDateTime createdAt;
    private BranchDto branch;
    private UserDto cashier;
    private Customer customer;
    private PaymentType paymentType;
    private OrderStatus status;
    private List<OrderItemDto> orderItems;
}
