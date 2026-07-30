package com.molla.service;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.payload.dto.OrderDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDto) throws Exception;
    OrderDto updateOrder(Long id, OrderDto orderDto) throws Exception;
    void deleteOrder(Long id) throws Exception;
    OrderDto getOrderById(Long id) throws Exception;
    List<OrderDto> getOrdersByBranch(Long branchId, Long customerId, Long cashierId, PaymentType paymentType, OrderStatus orderStatus) throws Exception;
    List<OrderDto> getOrdersByCashier(Long cashierId) throws Exception;
    List<OrderDto> getTodayOrderbyBranch(Long branchId) throws Exception;
    List<OrderDto> getOrderByCustomerId(Long customerId) throws Exception;
    List<OrderDto> getTop5RecentOrdersByBranchId(Long branchId) throws Exception;

    /**
     * Paginated + sorted orders listing for a branch.
     */
    Page<OrderDto> getOrdersByBranchPaged(Long branchId, int page, int size, String sortBy, String direction) throws Exception;
}
