package com.molla.service.impl;

import com.molla.exceptions.BadRequestException;
import com.molla.repository.OrderRepository;
import com.molla.repository.RefundRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSalesPeriodTest {
    @Mock OrderRepository orderRepository;
    @Mock RefundRepository refundRepository;
    @Mock UserService userService;
    @Mock AccessControlService accessControlService;
    @InjectMocks OrderServiceImpl service;

    @Test
    void thirtyDaysIncludesOlderSalesAndFillsMissingDays() {
        LocalDate today = LocalDate.now();
        when(orderRepository.findSalesRowsBetween(any(), any())).thenReturn(
                Collections.singletonList(new Object[]{today.minusDays(20).atTime(12, 0), 1250.0}));

        var result = service.getSystemSalesSummary(30);

        assertEquals(30, result.getDailySales().size());
        assertEquals(today.minusDays(29).toString(), result.getDailySales().get(0).getName());
        assertEquals(today.toString(), result.getDailySales().get(29).getName());
        assertEquals(1250.0, result.getDailySales().get(9).getAmount());
        assertEquals(0.0, result.getDailySales().get(0).getAmount());
        verify(orderRepository).findSalesRowsBetween(today.minusDays(29).atStartOfDay(), today.plusDays(1).atStartOfDay());
        verify(accessControlService).requireAdmin(any());
    }

    @Test
    void existingCallStillReturnsSevenDays() {
        assertEquals(7, service.getSystemSalesSummary().getDailySales().size());
    }

    @Test
    void unsupportedPeriodIsRejectedBeforeQuerying() {
        assertThrows(BadRequestException.class, () -> service.getSystemSalesSummary(365));
        verifyNoInteractions(orderRepository, refundRepository);
    }
}
