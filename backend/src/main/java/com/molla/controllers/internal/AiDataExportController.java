package com.molla.controllers.internal;

import com.molla.payload.dto.ai.AiTransactionDto;
import com.molla.service.AiDataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class AiDataExportController {

    private  final AiDataExportService aiDataExportService;
    @GetMapping("/stores/{storeId}/transactions")
    public List<AiTransactionDto> exportTransactions(
            @PathVariable Long storeId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime to
    ) {
        return aiDataExportService
                .exportCompletedTransactions(
                        storeId,
                        from,
                        to
                );
    }

}
