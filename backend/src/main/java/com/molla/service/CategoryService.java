package com.molla.service;

import com.molla.model.User;
import com.molla.payload.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto, User user);

    CategoryDto updateCategory(Long id, CategoryDto categoryDto, User user);

    void deleteCategory(Long id, User user);

    List<CategoryDto> getCategoriesByStoreId(Long storeId);
    
    CategoryDto moderateCategory(Long id, CategoryDto categoryDto, User user);
}
