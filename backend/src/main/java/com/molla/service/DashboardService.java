package com.molla.service;

import com.molla.payload.dto.StoreDashboardDto;

/**
 * Aggregated, store-scoped read model behind the store-admin dashboard.
 * All figures are derived from stored orders — nothing is fabricated.
 */
public interface DashboardService {

    /**
     * @param storeId     store whose orders are aggregated
     * @param period      TODAY | 7D | 30D | 3M | 1Y (null/unknown falls back to 7D)
     * @param granularity HOURLY | DAILY | WEEKLY | MONTHLY for the trend charts
     *                    (null/unknown falls back to the period default)
     */
    StoreDashboardDto getStoreDashboard(Long storeId, String period, String granularity);

    /**
     * Same aggregate as {@link #getStoreDashboard} but scoped to one branch, plus
     * branch-only figures (payment split, active staff, refunds).
     *
     * @param branchId branch whose orders are aggregated
     */
    StoreDashboardDto getBranchDashboard(Long branchId, String period, String granularity);
}
