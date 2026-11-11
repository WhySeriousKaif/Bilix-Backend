package com.molla.mapper;

import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.payload.dto.ai.AiOrderItemDto;
import com.molla.payload.dto.ai.AiTransactionDto;

import java.math.BigDecimal;

public class AiTransactionMapper {

    private AiTransactionMapper() {
    }

    public static AiTransactionDto toDto(Order order){

        return new AiTransactionDto(
                order.getId(),
                order.getBranch().getStore().getId(),
                order.getBranch().getId(),
                order.getCreatedAt(),
                order.getPaymentType()==null ?
                        null
                        :
                        order.getPaymentType().name(),
                money(order.getTotalAmount()),
                order.getOrderItems().stream().map(AiTransactionMapper::toItemDto).toList()


        );


    }

    private static AiOrderItemDto toItemDto(OrderItem item){
        var product=item.getProduct();
        var category=product.getCategory();

        return new AiOrderItemDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                category==null ? null : category.getId(),
                category == null ? null : category.getName(),
                item.getQuantity(),
                money(item.getPrice()),
                money(item.getTotalPrice())

        );
    }
    private  static BigDecimal money(Double value){
        return value==null
                ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
