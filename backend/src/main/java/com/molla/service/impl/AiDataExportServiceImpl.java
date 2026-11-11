package com.molla.service.impl;

import com.molla.exceptions.BadRequestException;
import com.molla.mapper.AiTransactionMapper;
import com.molla.payload.dto.ai.AiTransactionDto;
import com.molla.repository.OrderRepository;
import com.molla.service.AiDataExportService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiDataExportServiceImpl implements AiDataExportService {

    private  final OrderRepository orderRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private static final long MAX_EXPORT_DAYS = 31;

    @Override
    @Transactional(readOnly = true)
    public List<AiTransactionDto> exportCompletedTransactions(Long storeId, LocalDateTime from, LocalDateTime to)
             {

        if (storeId == null) {
            throw new BadRequestException(
                    "Store id is required"
            );
        }

        if (from == null || to == null) {
            throw new BadRequestException(
                    "Both from and to dates are required"
            );
        }
        if(!from.isBefore(to)){
            throw  new BadRequestException(
                    " 'from' must be before 'to'"
            );
        }
        long requestedDays= ChronoUnit.DAYS.between(from,to);

        if(requestedDays>MAX_EXPORT_DAYS){
            throw  new BadRequestException(  "A single export cannot exceed 31 days");
        }

        var currentUser = userService.getCurrentUser();

        accessControlService.requireManagerAccessToStore(
                currentUser,
                storeId
        );

        return orderRepository.findCompletedOrdersForAiExport(storeId,from,to)
                .stream()
                .map(AiTransactionMapper::toDto)
                .toList();
    }
}
