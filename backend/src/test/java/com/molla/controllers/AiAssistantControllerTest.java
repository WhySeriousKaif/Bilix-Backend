package com.molla.controllers;

import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.ai.AiAssistantRequest;
import com.molla.payload.dto.ai.AssistantChatRequestDto;
import com.molla.payload.dto.ai.AssistantResponseDto;
import com.molla.repository.BranchRepository;
import com.molla.service.AiAssistantService;
import com.molla.service.UserService;
import com.molla.service.impl.AccessControlService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantControllerTest {

    @Mock
    private AiAssistantService aiAssistantService;

    @Mock
    private UserService userService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private AiAssistantController controller;

    @Test
    void derivesStoreFromAuthorizedBranch() {
        User currentUser = new User();

        Store store = new Store();
        store.setId(7L);

        Branch branch = new Branch();
        branch.setId(3L);
        branch.setStore(store);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(branchRepository.findById(3L)).thenReturn(Optional.of(branch));
        when(aiAssistantService.chat(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        ))
                .thenReturn(AssistantResponseDto.unavailable());

        controller.chat(
                "Bearer manager-token",
                new AssistantChatRequestDto(
                        3L,
                        "What products sell with coffee?",
                        List.of()
                )
        );

        verify(accessControlService)
                .requireManagerAccessToBranch(currentUser, branch);

        ArgumentCaptor<AiAssistantRequest> requestCaptor =
                ArgumentCaptor.forClass(AiAssistantRequest.class);

        verify(aiAssistantService).chat(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("Bearer manager-token")
        );

        AiAssistantRequest forwarded = requestCaptor.getValue();
        assertThat(forwarded.storeId()).isEqualTo(7L);
        assertThat(forwarded.branchId()).isEqualTo(3L);
        assertThat(forwarded.message())
                .isEqualTo("What products sell with coffee?");
    }
}
