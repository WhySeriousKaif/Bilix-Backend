package com.molla.service;

import com.molla.exceptions.BadRequestException;
import com.molla.payload.dto.ai.AiTransactionDto;

import java.time.LocalDateTime;
import java.util.List;

public interface AiDataExportService {

    List<AiTransactionDto>exportCompletedTransactions(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) ;
}
