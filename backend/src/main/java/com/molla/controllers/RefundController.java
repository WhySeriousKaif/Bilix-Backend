package com.molla.controllers;

import com.molla.payload.dto.RefundDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundDto> createRefund(@RequestBody RefundDto refundDto) throws Exception {
        RefundDto refund = refundService.createRefund(refundDto);
        return ResponseEntity.ok(refund);
    }

    @GetMapping
    public ResponseEntity<List<RefundDto>> getAllRefund() throws Exception {
        List<RefundDto> refunds = refundService.getAllRefunds();
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<RefundDto>> getRefundByCashier(@PathVariable("cashierId") Long cashierId) throws Exception {
        List<RefundDto> refunds = refundService.getRefundByCashier(cashierId);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<RefundDto>> getRefundByBranch(@PathVariable("branchId") Long branchId) throws Exception {
        List<RefundDto> refunds = refundService.getRefundByBranch(branchId);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/shift/{shiftReportId}")
    public ResponseEntity<List<RefundDto>> getRefundByShift(@PathVariable("shiftReportId") Long shiftReportId) throws Exception {
        List<RefundDto> refunds = refundService.getRefundByShiftReport(shiftReportId);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/cashier/{cashierId}/range")
    public ResponseEntity<List<RefundDto>> getRefundByCashierAndDateRange(
            @PathVariable("cashierId") Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {
        List<RefundDto> refunds = refundService.getRefundByCashierAndDateRange(cashierId, from, to);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundDto> getRefundById(@PathVariable("id") Long id) throws Exception {
        RefundDto refund = refundService.getRefundById(id);
        return ResponseEntity.ok(refund);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRefund(@PathVariable("id") Long id) throws Exception {
        refundService.deleteRefund(id);
        ApiResponse apiResponse = new ApiResponse("Refund deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}

