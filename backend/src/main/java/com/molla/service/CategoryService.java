package com.molla.service;

import com.molla.payload.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto);

    CategoryDto updateCategory(Long id, CategoryDto categoryDto);

    void deleteCategory(Long id);

    List<CategoryDto> getCategoriesByStoreId(Long storeId);
    
    CategoryDto moderateCategory(Long id, CategoryDto categoryDto);
}
