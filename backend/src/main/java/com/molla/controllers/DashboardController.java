package com.molla.controllers;

import com.molla.payload.dto.StoreDashboardDto;
import com.molla.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only aggregates for the store-admin dashboard.
 *
 * <p>Access is enforced in the service layer: a store admin may only ask for their own
 * store, a super admin for any store.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Example: GET /api/dashboard/store/1?period=7D&granularity=DAILY
     *
     * @param period      TODAY | 7D | 30D | 3M | 1Y (defaults to 7D)
     * @param granularity HOURLY | DAILY | WEEKLY | MONTHLY (defaults to the period's bucketing)
     */
    @GetMapping("/store/{storeId}")
    public ResponseEntity<StoreDashboardDto> getStoreDashboard(
            @PathVariable("storeId") Long storeId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String granularity) {
        return ResponseEntity.ok(dashboardService.getStoreDashboard(storeId, period, granularity));
    }
}
