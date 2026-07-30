package com.molla.mapper;

import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;

public class StoreMapper {

    public static StoreDto toDTO(Store store){
        StoreDto storeDto = new StoreDto();
        storeDto.setId(store.getId());
        storeDto.setBrand(store.getBrand());
        storeDto.setDescription(store.getDescription());
        
        if(store.getStoreAdmin() != null) {
            storeDto.setStoreAdmin(UserMapper.toDto(store.getStoreAdmin()));
        }
        
        storeDto.setStoreType(store.getStoreType());
        storeDto.setContact(store.getContact());
        storeDto.setCreatedAt(store.getCreatedAt());
        storeDto.setUpdatedAt(store.getUpdatedAt());
        storeDto.setStoreStatus(store.getStoreStatus());

        return storeDto;
    }

    public static Store toEntity(StoreDto storeDto, User user){
        Store store = new Store();
        store.setId(storeDto.getId());
        store.setBrand(storeDto.getBrand());
        store.setDescription(storeDto.getDescription());
        store.setStoreAdmin(user);
        store.setStoreType(storeDto.getStoreType());
        store.setContact(storeDto.getContact());
        store.setCreatedAt(storeDto.getCreatedAt());
        store.setUpdatedAt(storeDto.getUpdatedAt());
        store.setStoreStatus(storeDto.getStoreStatus());

        return store;
    }

}
