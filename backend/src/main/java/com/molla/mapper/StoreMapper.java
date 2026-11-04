package com.molla.mapper;

import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;
import com.molla.domain.StoreStatus;

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
        store.setBrand(storeDto.getBrand());
        store.setDescription(storeDto.getDescription());
        store.setStoreAdmin(user);
        store.setStoreType(storeDto.getStoreType());
        store.setContact(storeDto.getContact());
        // Identity, timestamps, and moderation state are owned by the server.
        store.setStoreStatus(StoreStatus.PENDING);

        return store;
    }

}
