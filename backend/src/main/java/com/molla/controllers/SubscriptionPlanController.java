package com.molla.controllers;

import com.molla.payload.dto.SubscriptionPlanDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @PostMapping
    public ResponseEntity<SubscriptionPlanDto> createPlan(@RequestBody SubscriptionPlanDto planDto) {
        return new ResponseEntity<>(subscriptionPlanService.createPlan(planDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanDto>> getAllPlans() {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanDto> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.getPlanById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlanDto> updatePlan(@PathVariable Long id, @RequestBody SubscriptionPlanDto planDto) {
        return ResponseEntity.ok(subscriptionPlanService.updatePlan(id, planDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.ok(new ApiResponse("Subscription plan deleted successfully"));
    }
}
