package com.molla.mapper;

import com.molla.model.Order;
import com.molla.payload.dto.OrderDto;
import java.util.stream.Collectors;

public class OrderMapper {
    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        return OrderDto.builder()
            .id(order.getId())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .branch(order.getBranch() != null ? BranchMapper.toDto(order.getBranch()) : null)
            .cashier(order.getCashier() != null ? UserMapper.toDto(order.getCashier()) : null)
            .customer(order.getCustomer())
            .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
            .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
            .cashierId(order.getCashier() != null ? order.getCashier().getId() : null)
            .paymentType(order.getPaymentType())
            .status(order.getStatus())
            .orderItems(order.getOrderItems() != null ? 
                order.getOrderItems().stream().map(OrderItemMapper::toDto).collect(Collectors.toList()) : 
                null)
            .build();
    }
}
