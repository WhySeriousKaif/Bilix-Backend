package com.molla.service;

import com.molla.payload.dto.SubscriptionPlanDto;

import java.util.List;

public interface SubscriptionPlanService {
    SubscriptionPlanDto createPlan(SubscriptionPlanDto planDto);
    SubscriptionPlanDto updatePlan(Long id, SubscriptionPlanDto planDto);
    void deletePlan(Long id);
    List<SubscriptionPlanDto> getAllPlans();
    SubscriptionPlanDto getPlanById(Long id);
}
