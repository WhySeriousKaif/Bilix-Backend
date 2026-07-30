package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.payload.dto.ShiftReportDto;
import com.molla.service.ShiftReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shift-reports")
@RequiredArgsConstructor
public class ShiftReportController {

    private final ShiftReportService shiftReportService;

    @PostMapping("/start")
    public ResponseEntity<ShiftReportDto> startShift(
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime shiftStart) throws Exception {
        ShiftReportDto shiftReport = shiftReportService.startShift(cashierId, branchId, shiftStart);
        return ResponseEntity.ok(shiftReport);
    }

    @PatchMapping("/end")
    public ResponseEntity<ShiftReportDto> endShift(
            @RequestParam(required = false) Long shiftReportId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime shiftEnd) throws Exception {
        ShiftReportDto shiftReport = shiftReportService.endShift(shiftReportId, shiftEnd);
        return ResponseEntity.ok(shiftReport);
    }

    @GetMapping("/current")
    public ResponseEntity<ShiftReportDto> getCurrentShiftProgress(
            @RequestParam(required = false) Long cashierId) throws UserException {
        ShiftReportDto shiftReport = shiftReportService.getCurrentShiftProgress(cashierId);
        return ResponseEntity.ok(shiftReport);
    }

    @GetMapping("/cashier/{cashierId}/by-date")
    public ResponseEntity<ShiftReportDto> getShiftByCashierAndDate(
            @PathVariable("cashierId") Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) throws UserException {
        ShiftReportDto shiftReport = shiftReportService.getShiftByCashierAndDate(cashierId, date);
        return ResponseEntity.ok(shiftReport);
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<ShiftReportDto>> getShiftReportByCashier(
            @PathVariable("cashierId") Long cashierId) throws Exception {
        List<ShiftReportDto> shiftReports = shiftReportService.getShiftReportsByCashierId(cashierId);
        return ResponseEntity.ok(shiftReports);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ShiftReportDto>> getShiftReportsByBranch(
            @PathVariable("branchId") Long branchId) throws Exception {
        List<ShiftReportDto> shiftReports = shiftReportService.getShiftReportsByBranchId(branchId);
        return ResponseEntity.ok(shiftReports);
    }

    @GetMapping
    public ResponseEntity<List<ShiftReportDto>> getAllShiftReports() throws Exception {
        List<ShiftReportDto> shiftReports = shiftReportService.getAllShiftReports();
        return ResponseEntity.ok(shiftReports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftReportDto> getShiftReportById(@PathVariable("id") Long id) throws Exception {
        ShiftReportDto shiftReport = shiftReportService.getShiftReportById(id);
        return ResponseEntity.ok(shiftReport);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteShiftReport(@PathVariable("id") Long id) throws Exception {
        // Note: Delete functionality not in service interface, but can be added if needed
        return ResponseEntity.ok("Delete functionality not implemented");
    }
}

