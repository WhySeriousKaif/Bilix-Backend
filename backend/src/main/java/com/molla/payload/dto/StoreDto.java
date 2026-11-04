package com.molla.payload.dto;

import com.molla.domain.StoreStatus;
import com.molla.model.StoreContact;
import lombok.Data;

import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;


@Data
public class StoreDto {

    public Long id;

    @NotBlank(message = "Store brand is required")
    public String brand;

    private UserDto  storeAdmin; //one store has one admin

    private CategoryDto category;

    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;

    private String Description;

    private String storeType;

    private StoreStatus storeStatus;


    @Valid
    private StoreContact contact;
}
