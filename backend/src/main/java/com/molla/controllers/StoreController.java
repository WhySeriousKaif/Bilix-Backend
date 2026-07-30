package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;
import com.molla.payload.dto.StoreStatusDto;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.StoreService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<StoreDto> createStore(@RequestBody StoreDto storeDto,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(storeService.createStore(storeDto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStoreById(@PathVariable("id") Long id,
            @RequestHeader("Authorization") String jwt) throws UserException {

        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @GetMapping
    public ResponseEntity<List<StoreDto>> getAllStores(@RequestHeader("Authorization") String jwt)
            throws UserException {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/admin")
    public ResponseEntity<List<StoreDto>> getStoreByAdmin(@RequestHeader("Authorization") String jwt)
            throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(storeService.getStoresForAdmin(user));
    }

    @GetMapping("/employee")
    public ResponseEntity<StoreDto> getStoreByEmployee(@RequestHeader("Authorization") String jwt)
            throws UserException {
        User user = userService.getUserFromJwt(jwt);
        String brand = user.getStore() != null ? user.getStore().getBrand() : "";
        return ResponseEntity.ok(storeService.getStoreByEmployee(brand));
    }

    @GetMapping("/my")
    public ResponseEntity<StoreDto> getMyStore(@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(storeService.getMyStore(user));
    }

    @GetMapping("/my/employees")
    public ResponseEntity<List<UserDto>> getMyStoreEmployees(@RequestHeader("Authorization") String jwt)
            throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(storeService.getMyStoreEmployees(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDto> updateStore(@PathVariable("id") Long id, @RequestBody StoreDto storeDto,
            @RequestHeader("Authorization") String jwt) throws UserException {
        return ResponseEntity.ok(storeService.updateStore(id, storeDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStore(@PathVariable("id") Long id,
            @RequestHeader("Authorization") String jwt) throws UserException {
        storeService.deleteStore(id);
        ApiResponse apiResponse = new ApiResponse("Store deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}/moderate")
    public ResponseEntity<StoreDto> moderateStore(
            @PathVariable("id") Long id,
            @RequestBody StoreStatusDto storeStatusDto,
            @RequestHeader("Authorization") String jwt) throws UserException {
        // Validate JWT token - service layer will check if user is Super Admin
        userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(storeService.moderateStore(id, storeStatusDto.getStatus()));
    }

}
