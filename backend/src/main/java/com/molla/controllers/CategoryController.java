package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.dto.CategoryDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.CategoryService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto categoryDto,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(categoryService.createCategory(categoryDto,user));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<CategoryDto>> getCategoriesByStoreId(@PathVariable("storeId") Long storeId,@RequestHeader("Authorization") String jwt) throws UserException {
        return ResponseEntity.ok(categoryService.getCategoriesByStoreId(storeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable("id") Long id,@RequestBody CategoryDto categoryDto,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(categoryService.updateCategory(id,categoryDto,user));
    }
    @PutMapping("/{id}/moderate")
    public ResponseEntity<CategoryDto> moderateCategory(@PathVariable("id") Long id,@RequestBody CategoryDto categoryDto,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(categoryService.moderateCategory(id,categoryDto,user));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable("id") Long id,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        categoryService.deleteCategory(id, user);
        ApiResponse apiResponse = new ApiResponse("Category deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }   

    
    
    
    
}
