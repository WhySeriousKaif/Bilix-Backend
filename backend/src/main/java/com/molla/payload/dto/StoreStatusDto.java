package com.molla.payload.dto;

import com.molla.domain.StoreStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusDto {
    private StoreStatus status;
}
