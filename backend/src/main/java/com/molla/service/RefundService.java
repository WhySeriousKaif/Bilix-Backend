package com.molla.service;

import com.molla.payload.dto.RefundDto;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundService {
    
    RefundDto createRefund(RefundDto refundDto) throws Exception;
    
    List<RefundDto> getAllRefunds() throws Exception;
    
    List<RefundDto> getRefundByCashier(Long cashierId) throws Exception;
    
    List<RefundDto> getRefundByShiftReport(Long shiftReportId) throws Exception;
    
    List<RefundDto> getRefundByCashierAndDateRange(Long cashierId, LocalDateTime startDate, LocalDateTime endDate) throws Exception;
    
    List<RefundDto> getRefundByBranch(Long branchId) throws Exception;
    
    RefundDto getRefundById(Long refundId) throws Exception;
    
    void deleteRefund(Long refundId) throws Exception;
}

