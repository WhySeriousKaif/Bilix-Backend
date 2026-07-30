package com.molla.service;

import com.molla.domain.StoreStatus;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;

import java.util.List;

public interface StoreService {

   StoreDto createStore(StoreDto storeDto, User user);

   StoreDto getStoreById(Long id);

   List<StoreDto> getAllStores();

   StoreDto getStoreByAdmin(User user);

   /** Super admin: all stores; store admin: their store only. */
   List<StoreDto> getStoresForAdmin(User user);

   StoreDto updateStore(Long id, StoreDto storeDto);

   void deleteStore(Long id);
   
   StoreDto getStoreByEmployee(String brand);

   StoreDto moderateStore(Long id, StoreStatus storeStatus);

   /** Get store admin's own store details. */
   StoreDto getMyStore(User user);

   /** Get all employees (cashiers, etc.) in store admin's store. */
   List<com.molla.payload.dto.UserDto> getMyStoreEmployees(User user) throws com.molla.exceptions.UserException;
}
