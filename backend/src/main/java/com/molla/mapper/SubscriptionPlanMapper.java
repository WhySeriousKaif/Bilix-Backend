package com.molla.mapper;

import com.molla.model.SubscriptionPlan;
import com.molla.payload.dto.SubscriptionPlanDto;

public class SubscriptionPlanMapper {

    public static SubscriptionPlanDto toDto(SubscriptionPlan plan) {
        if (plan == null) {
            return null;
        }
        SubscriptionPlanDto dto = new SubscriptionPlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPrice(plan.getPrice());
        dto.setMaxBranches(plan.getMaxBranches());
        dto.setMaxUsers(plan.getMaxUsers());
        dto.setMaxProducts(plan.getMaxProducts());
        dto.setFeatures(plan.getFeatures());
        dto.setActive(plan.getActive());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    public static SubscriptionPlan toEntity(SubscriptionPlanDto dto) {
        return SubscriptionPlan.builder()
                .name(dto.getName())
                .price(dto.getPrice())
                .maxBranches(dto.getMaxBranches())
                .maxUsers(dto.getMaxUsers())
                .maxProducts(dto.getMaxProducts())
                .features(dto.getFeatures())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}
