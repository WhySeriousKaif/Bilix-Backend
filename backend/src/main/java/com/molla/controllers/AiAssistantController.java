package com.molla.controllers;

import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Branch;
import com.molla.model.User;
import com.molla.payload.dto.ai.AiAssistantRequest;
import com.molla.payload.dto.ai.AssistantChatRequestDto;
import com.molla.payload.dto.ai.AssistantResponseDto;
import com.molla.payload.dto.ai.SystemAssistantChatRequestDto;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.OrderService;
import com.molla.repository.BranchRepository;
import com.molla.service.AiAssistantService;
import com.molla.service.UserService;
import com.molla.service.impl.AccessControlService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantResponseDto> chat(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody AssistantChatRequestDto request
    ) {
        User currentUser = userService.getCurrentUser();

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(
                        () -> new NotFoundException(
                                "Branch not found with id: " + request.branchId()
                        )
                );

        accessControlService.requireManagerAccessToBranch(
                currentUser,
                branch
        );

        if (branch.getStore() == null || branch.getStore().getId() == null) {
            throw new BadRequestException("Branch is not assigned to a store");
        }

        AiAssistantRequest aiRequest = new AiAssistantRequest(
                branch.getStore().getId(),
                branch.getId(),
                request.message(),
                request.conversation() == null
                        ? List.of()
                        : request.conversation(),
                "manager",
                null
        );

        return ResponseEntity.ok(
                aiAssistantService.chat(aiRequest, authorization)
        );
    }

    @PostMapping("/system/chat")
    public ResponseEntity<AssistantResponseDto> systemChat(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody SystemAssistantChatRequestDto request
    ) {
        User currentUser = userService.getCurrentUser();
        accessControlService.requireAdmin(currentUser);

        var stores = storeRepository.findAll();
        Map<String, Long> storeStatuses = stores.stream()
                .collect(Collectors.groupingBy(
                        store -> store.getStoreStatus() == null
                                ? "UNKNOWN"
                                : store.getStoreStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("sales", orderService.getSystemSalesSummary());
        context.put("totalStores", stores.size());
        context.put("storeStatuses", storeStatuses);
        context.put("totalUsers", userRepository.count());
        context.put("scope", "Bilix platform-wide read-only oversight");

        AiAssistantRequest aiRequest = new AiAssistantRequest(
                null,
                null,
                request.message(),
                request.conversation() == null ? List.of() : request.conversation(),
                "system",
                context
        );

        return ResponseEntity.ok(aiAssistantService.chat(aiRequest, authorization));
    }
}
