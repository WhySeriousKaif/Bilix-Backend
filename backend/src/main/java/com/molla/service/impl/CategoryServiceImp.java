package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.CategoryMapper;
import com.molla.model.Category;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.CategoryDto;
import com.molla.repository.CategoryRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.CategoryService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public CategoryDto createCategory(CategoryDto categoryDto, User user) {
        Store store = storeRepository.findById(categoryDto.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + categoryDto.getStoreId()));
        checkAuthority(user, store);

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setStore(store);

        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "categoriesByStore", key = "#storeId")
    public List<CategoryDto> getCategoriesByStoreId(Long storeId) {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        List<Category> categories = categoryRepository.findByStoreId(storeId);
        return categories.stream().map(CategoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = "categoriesByStore", allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        checkAuthority(user, category.getStore()); 

        return CategoryMapper.toDto(categoryRepository.save(category));
    }
    
    @Override
    public CategoryDto moderateCategory(Long id, CategoryDto categoryDto, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        checkAuthority(user, category.getStore());
        
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }
    
    @Override
    @CacheEvict(cacheNames = "categoriesByStore", allEntries = true)
    public void deleteCategory(Long id, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        checkAuthority(user, category.getStore()); 
        categoryRepository.delete(category);
    }

    private void checkAuthority(User user, Store store) {
        if (store == null) {
            throw new NotFoundException("Store not found");
        }
        // Was previously checking "is this user *a* store admin/manager" (any store),
        // not "is this user the admin/manager *of this store*" — letting any store admin
        // touch any other store's categories. requireStoreAccess scopes it correctly.
        accessControlService.requireStoreAccess(user, store.getId());
    }
}
