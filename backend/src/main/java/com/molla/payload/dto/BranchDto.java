package com.molla.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {

   
    private Long id;
    private String name;

    private  String phone;

    private String address;

    private String email;


    private List<String> workingDays;

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StoreDto store; //many branches belong to one store

    private Long storeId;

    private  UserDto manager; //one branch has one manager
    
}
