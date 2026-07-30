package com.molla.service;

import com.molla.exceptions.UserException;
import com.molla.payload.dto.ShiftReportDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftReportService {
    
    ShiftReportDto startShift(Long cashierId, Long branchId, LocalDateTime shiftStart) throws Exception;
    
    ShiftReportDto endShift(Long shiftReportId, LocalDateTime shiftEnd) throws Exception;
    
    ShiftReportDto getShiftReportById(Long id) throws Exception;
    
    List<ShiftReportDto> getAllShiftReports() throws Exception;
    
    List<ShiftReportDto> getShiftReportsByBranchId(Long branchId) throws Exception;
    
    List<ShiftReportDto> getShiftReportsByCashierId(Long cashierId) throws Exception;
    
    ShiftReportDto getCurrentShiftProgress(Long cashierId) throws UserException;
    
    ShiftReportDto getShiftByCashierAndDate(Long cashierId, LocalDateTime date) throws UserException;
}

