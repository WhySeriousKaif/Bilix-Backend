package com.molla.mapper;

import com.molla.model.Category;
import com.molla.payload.dto.CategoryDto;

public class CategoryMapper {
    
    public static CategoryDto toDto(Category category) {
        if(category == null) {
            return null;
        }
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        categoryDto.setDescription(category.getDescription());
        categoryDto.setStoreId(category.getStore() != null ? category.getStore().getId() : null);
        return categoryDto;
    }
    
    public static Category toEntity(CategoryDto categoryDto) {
        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        return category;
    }
}
