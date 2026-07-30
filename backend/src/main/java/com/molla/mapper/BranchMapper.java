package com.molla.mapper;

import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.payload.dto.BranchDto;

public class BranchMapper {

    public static BranchDto toDto(Branch branch) {
        return BranchDto.builder()
        .id(branch.getId())
        .name(branch.getName())
        .phone(branch.getPhone())
        .address(branch.getAddress())
        .email(branch.getEmail())
        .workingDays(branch.getWorkingDays())
        .openTime(branch.getOpenTime())
        .closeTime(branch.getCloseTime())
        .createdAt(branch.getCreatedAt())
        .updatedAt(branch.getUpdatedAt())
        .storeId(branch.getStore()!=null ? branch.getStore().getId() : null)
        .build();
    }
    public static Branch toEntity(BranchDto branchDto,Store store) {
        return Branch.builder()
        .id(branchDto.getId())
        .name(branchDto.getName())
        .phone(branchDto.getPhone())
        .address(branchDto.getAddress())
        .store(store)
        .email(branchDto.getEmail())
        .workingDays(branchDto.getWorkingDays())
        .openTime(branchDto.getOpenTime())
        .closeTime(branchDto.getCloseTime())
        .createdAt(branchDto.getCreatedAt())
        .updatedAt(branchDto.getUpdatedAt())
        .build();
    }
}
