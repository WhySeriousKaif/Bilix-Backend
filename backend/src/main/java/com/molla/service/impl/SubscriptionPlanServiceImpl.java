package com.molla.service.impl;

import com.molla.exceptions.NotFoundException;
import com.molla.mapper.SubscriptionPlanMapper;
import com.molla.model.SubscriptionPlan;
import com.molla.payload.dto.SubscriptionPlanDto;
import com.molla.repository.SubscriptionPlanRepository;
import com.molla.service.SubscriptionPlanService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public SubscriptionPlanDto createPlan(SubscriptionPlanDto planDto) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        SubscriptionPlan plan = SubscriptionPlanMapper.toEntity(planDto);
        return SubscriptionPlanMapper.toDto(subscriptionPlanRepository.save(plan));
    }

    @Override
    public SubscriptionPlanDto updatePlan(Long id, SubscriptionPlanDto planDto) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with id: " + id));

        plan.setName(planDto.getName());
        plan.setPrice(planDto.getPrice());
        plan.setMaxBranches(planDto.getMaxBranches());
        plan.setMaxUsers(planDto.getMaxUsers());
        plan.setMaxProducts(planDto.getMaxProducts());
        plan.setFeatures(planDto.getFeatures());
        if (planDto.getActive() != null) {
            plan.setActive(planDto.getActive());
        }

        return SubscriptionPlanMapper.toDto(subscriptionPlanRepository.save(plan));
    }

    @Override
    public void deletePlan(Long id) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with id: " + id));
        subscriptionPlanRepository.delete(plan);
    }

    @Override
    public List<SubscriptionPlanDto> getAllPlans() {
        return subscriptionPlanRepository.findAll().stream()
                .map(SubscriptionPlanMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionPlanDto getPlanById(Long id) {
        return subscriptionPlanRepository.findById(id)
                .map(SubscriptionPlanMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found with id: " + id));
    }
}
