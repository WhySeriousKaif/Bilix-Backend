package com.molla.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserRoleTest {

    @Test
    public void testDeserializeCashier() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = "\"Cashier\"";
        UserRole role = mapper.readValue(json, UserRole.class);
        assertEquals(UserRole.ROLE_CASHIER, role);
    }

    @Test
    public void testDeserializeBranchManager() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = "\"Branch Manager\"";
        UserRole role = mapper.readValue(json, UserRole.class);
        assertEquals(UserRole.ROLE_BRANCH_MANAGER, role);
    }
}
