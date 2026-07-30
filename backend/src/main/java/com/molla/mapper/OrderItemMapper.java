package com.molla.mapper;

import com.molla.model.OrderItem;
import com.molla.payload.dto.OrderItemDto;

public class OrderItemMapper {
    public static OrderItemDto toDto(OrderItem orderItem) {
        if(orderItem == null){
            return null;
        }
        return OrderItemDto.builder()
            .id(orderItem.getId())
            .quantity(orderItem.getQuantity())
            .price(orderItem.getPrice())
            .product(orderItem.getProduct() != null ? ProductMapper.toDto(orderItem.getProduct()) : null)
            .orderId(orderItem.getOrder() != null ? orderItem.getOrder().getId() : null)
            .build();
    }
}
