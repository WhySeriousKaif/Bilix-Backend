package com.molla.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level security regression tests.
 *
 * These tests intentionally exercise the real Spring Security filter chain, JWT parser,
 * controllers, and resource-level authorization instead of calling services directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StoreRepository storeRepository;
    @Autowired private BranchRepository branchRepository;

    private Long ownBranchId;
    private Long otherBranchId;

    @BeforeEach
    void createSecondBranchForCrossBranchTest() {
        Store store = storeRepository.findAll().stream().findFirst().orElseThrow();
        Branch ownBranch = branchRepository.findAll().stream().findFirst().orElseThrow();
        ownBranchId = ownBranch.getId();

        Branch otherBranch = new Branch();
        otherBranch.setName("Security Test Branch");
        otherBranch.setAddress("Isolated branch used by authorization tests");
        otherBranch.setStore(store);
        otherBranchId = branchRepository.save(otherBranch).getId();
    }

    @Test
    void protectedEndpointRejectsMissingJwt() throws Exception {
        mockMvc.perform(get("/api/orders/branch/{branchId}", ownBranchId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMalformedJwt() throws Exception {
        mockMvc.perform(get("/api/orders/branch/{branchId}", ownBranchId)
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validCashierJwtCanAccessAssignedBranch() throws Exception {
        mockMvc.perform(get("/api/orders/branch/{branchId}", ownBranchId)
                        .header("Authorization", "Bearer " + cashierJwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"));
    }

    @Test
    void cashierCannotAccessAnotherBranchByChangingPathId() throws Exception {
        mockMvc.perform(get("/api/orders/branch/{branchId}", otherBranchId)
                        .header("Authorization", "Bearer " + cashierJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("do not have access")));
    }

    @Test
    void publicSignupCannotCreateSuperAdmin() throws Exception {
        String request = """
                {
                  "fullName": "Attacker",
                  "email": "attacker@example.com",
                  "password": "StrongPassword123!",
                  "role": "ROLE_ADMIN"
                }
                """;

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot self-register")));
    }

    private String cashierJwt() throws Exception {
        String loginRequest = """
                {
                  "email": "cashier@molla.com",
                  "password": "Cashier123@"
                }
                """;

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("jwt").asText();
    }
}
