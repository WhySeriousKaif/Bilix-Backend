package com.molla.mapper;

import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.Refund;
import com.molla.model.ShiftReport;
import com.molla.payload.dto.OrderDto;
import com.molla.payload.dto.ProductDto;
import com.molla.payload.dto.RefundDto;
import com.molla.payload.dto.ShiftReportDto;

import java.util.List;
import java.util.stream.Collectors;

public class ShiftReportMapper {

    public static ShiftReportDto toDto(ShiftReport entity) {
        if (entity == null) {
            return null;
        }

        return ShiftReportDto.builder()
            .id(entity.getId())
            .shiftStart(entity.getShiftStart())
            .shiftEnd(entity.getShiftEnd())
            .totalSales(entity.getTotalSales())
            .totalRefunds(entity.getTotalRefunds())
            .netSale(entity.getNetSales())
            .totalOrders(entity.getTotalOrders())
            .cashier(entity.getCashier() != null ? UserMapper.toDto(entity.getCashier()) : null)
            .cashierId(entity.getCashier() != null ? entity.getCashier().getId() : null)
            .branch(entity.getBranch() != null ? BranchMapper.toDto(entity.getBranch()) : null)
            .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
            .paymentSummaries(entity.getPaymentSummaries())
            .topSellingProducts(mapProducts(entity.getTopSellingProducts()))
            .recentOrders(mapOrders(entity.getRecentOrders()))
            .refunds(mapRefunds(entity.getRefunds()))
            .createdAt(entity.getCreatedAt())
            .build();
    }

    private static List<RefundDto> mapRefunds(List<Refund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return null;
        }
        return refunds.stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    private static List<ProductDto> mapProducts(List<Product> topSellingProducts) {
        if (topSellingProducts == null || topSellingProducts.isEmpty()) {
            return null;
        }
        return topSellingProducts.stream()
            .map(ProductMapper::toDto)
            .collect(Collectors.toList());
    }

    private static List<OrderDto> mapOrders(List<Order> recentOrders) {
        if (recentOrders == null || recentOrders.isEmpty()) {
            return null;
        }
        return recentOrders.stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }
}

