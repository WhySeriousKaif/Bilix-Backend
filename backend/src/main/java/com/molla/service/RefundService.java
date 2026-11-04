package com.molla.service;

import com.molla.payload.dto.RefundDto;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundService {
    
    RefundDto createRefund(RefundDto refundDto) throws Exception;
    
    List<RefundDto> getAllRefunds() throws Exception;
    
    List<RefundDto> getRefundByCashier(Long cashierId) throws Exception;
    
    List<RefundDto> getRefundByShiftReport(Long shiftReportId) throws Exception;
    
    List<RefundDto> getRefundByCashierAndDateRange(Long cashierId, LocalDateTime startDate, LocalDateTime endDate) throws Exception;
    
    List<RefundDto> getRefundByBranch(Long branchId) throws Exception;
    Page<RefundDto> getRefundByBranchPaged(Long branchId, Pageable pageable);
    Page<RefundDto> getRefundByBranchPaged(Long branchId, Pageable pageable,
            String search, String payment, String period);

    List<RefundDto> getRefundByOrder(Long orderId) throws Exception;
    
    RefundDto getRefundById(Long refundId) throws Exception;
    
    void deleteRefund(Long refundId) throws Exception;
}
