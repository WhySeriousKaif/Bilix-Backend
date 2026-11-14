package com.molla.payload.dto;

import com.molla.domain.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDto {
    private Long id;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 500, message = "Refund reason must be 500 characters or fewer")
    private String reason;

    // Accepted for compatibility but ignored: the refund amount is calculated on the server
    // from the original order price, so a client cannot decide how much money is returned.
    private Double amount;

    private Long shiftReportId;
    private UserDto cashier;
    private BranchDto branch;
    @NotNull(message = "Refund payment method is required")
    private PaymentType paymentType;

    private LocalDateTime createdAt;

    @Valid
    @NotEmpty(message = "Select at least one item to refund")
    private List<RefundItemDto> items;
}
