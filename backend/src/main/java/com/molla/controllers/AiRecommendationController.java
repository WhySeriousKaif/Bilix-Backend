package com.molla.controllers;

import com.molla.model.User;
import com.molla.payload.dto.ai.AiRecommendationRequest;
import com.molla.payload.dto.ai.RecommendationResponseDto;
import com.molla.service.AiRecommendationService;
import com.molla.service.UserService;
import com.molla.service.impl.AccessControlService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService
            aiRecommendationService;

    private final UserService userService;

    private final AccessControlService
            accessControlService;

    @PostMapping("/recommendations")
    public ResponseEntity<RecommendationResponseDto>
    recommend(
            @Valid
            @RequestBody
            AiRecommendationRequest request
    ) {
        User currentUser =
                userService.getCurrentUser();

        accessControlService.requireStoreAccess(
                currentUser,
                request.storeId()
        );

        RecommendationResponseDto response =
                aiRecommendationService.recommend(
                        request
                );

        return ResponseEntity.ok(response);
    }
}