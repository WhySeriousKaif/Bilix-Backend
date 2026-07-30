package com.molla.payload.dto;

import com.molla.domain.StoreStatus;
import com.molla.model.StoreContact;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class StoreDto {

    public Long id;

    public  String brand;

    private UserDto  storeAdmin; //one store has one admin

    private CategoryDto category;

    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;

    private String Description;

    private String storeType;

    private StoreStatus storeStatus;


    private StoreContact contact;
}
