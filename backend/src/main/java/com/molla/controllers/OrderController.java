package com.molla.controllers;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.payload.dto.OrderDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) throws Exception {
        return ResponseEntity.ok(orderService.createOrder(orderDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable("id") Long id,
            @RequestBody OrderDto orderDto) throws Exception {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable("id") Long id) throws Exception {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(new ApiResponse("Order deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable("id") Long id) throws Exception {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<OrderDto>> getOrdersByBranch(
            @PathVariable("branchId") Long branchId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) PaymentType paymentType,
            @RequestParam(required = false) OrderStatus orderStatus) throws Exception {
        return ResponseEntity.ok(orderService.getOrdersByBranch(branchId, customerId, cashierId, paymentType, orderStatus));
    }

    /**
     * Paginated + sorted orders listing for a branch.
     * Example: /api/orders/branch/1/paged?page=0&size=10&sortBy=createdAt&direction=desc
     */
    @GetMapping("/branch/{branchId}/paged")
    public ResponseEntity<Page<OrderDto>> getOrdersByBranchPaged(
            @PathVariable("branchId") Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) throws Exception {
        Page<OrderDto> result = orderService.getOrdersByBranchPaged(branchId, page, size, sortBy, direction);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<OrderDto>> getOrdersByCashier(@PathVariable("cashierId") Long cashierId) throws Exception {
        return ResponseEntity.ok(orderService.getOrdersByCashier(cashierId));
    }

    @GetMapping("/today/branch/{id}")
    public ResponseEntity<List<OrderDto>> getTodayOrder(@PathVariable("id") Long id) throws Exception {
        return ResponseEntity.ok(orderService.getTodayOrderbyBranch(id));
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<List<OrderDto>> getCustomersOrder(@PathVariable("id") Long id) throws Exception {
        return ResponseEntity.ok(orderService.getOrderByCustomerId(id));
    }

    @GetMapping("/recent/{branchId}")
    public ResponseEntity<List<OrderDto>> getRecentOrder(@PathVariable("branchId") Long branchId) throws Exception {
        return ResponseEntity.ok(orderService.getTop5RecentOrdersByBranchId(branchId));
    }
}
