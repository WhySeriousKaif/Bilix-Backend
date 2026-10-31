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
import com.molla.repository.ProductRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.CategoryService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final ProductRepository productRepository;

    @Override
    @CacheEvict(cacheNames = "categoriesByStore", allEntries = true)
    public CategoryDto createCategory(CategoryDto categoryDto, User user) {
        if (categoryDto.getStoreId() == null) {
            throw new BadRequestException("You need to create a store before adding categories.");
        }
        Store store = storeRepository.findById(categoryDto.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + categoryDto.getStoreId()));
        checkAuthority(user, store);

        String normalizedName = normalizeDisplayName(categoryDto.getName());
        if (normalizedName.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        Category existing = categoryRepository
                .findFirstByStoreIdAndNameIgnoreCase(store.getId(), normalizedName)
                .orElse(null);
        if (existing != null) {
            return toDtoWithCount(existing);
        }

        Category category = new Category();
        category.setName(normalizedName);
        category.setDescription(categoryDto.getDescription());
        category.setStore(store);

        return toDtoWithCount(categoryRepository.save(category));
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "categoriesByStore", key = "#storeId")
    @Transactional
    public List<CategoryDto> getCategoriesByStoreId(Long storeId) {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        return mergeDuplicateCategories(storeId).stream()
                .map(this::toDtoWithCount)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = "categoriesByStore", allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        String normalizedName = normalizeDisplayName(categoryDto.getName());
        categoryRepository.findFirstByStoreIdAndNameIgnoreCase(category.getStore().getId(), normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BadRequestException("Category already exists"); });
        category.setName(normalizedName);
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

    private List<Category> mergeDuplicateCategories(Long storeId) {
        Map<String, Category> canonicalByName = new LinkedHashMap<>();
        List<Category> scopedCategories = categoryRepository.findByStoreId(storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("Store not found"));
        for (Category unscoped : categoryRepository.findByStoreIsNull()) {
            var products = productRepository.findByCategoryId(unscoped.getId()).stream()
                    .filter(product -> product.getStore() != null && storeId.equals(product.getStore().getId()))
                    .toList();
            if (!products.isEmpty()) {
                unscoped.setStore(store);
                categoryRepository.save(unscoped);
                scopedCategories.add(unscoped);
            }
        }
        for (Category category : scopedCategories) {
            String cleanName = normalizeDisplayName(category.getName());
            String key = cleanName.toLowerCase(Locale.ROOT);
            Category canonical = canonicalByName.get(key);
            if (canonical == null) {
                category.setName(cleanName);
                canonicalByName.put(key, categoryRepository.save(category));
                continue;
            }

            var duplicateProducts = productRepository.findByCategoryId(category.getId());
            duplicateProducts.forEach(product -> product.setCategory(canonical));
            productRepository.saveAll(duplicateProducts);
            categoryRepository.delete(category);
        }
        return List.copyOf(canonicalByName.values());
    }

    private CategoryDto toDtoWithCount(Category category) {
        CategoryDto dto = CategoryMapper.toDto(category);
        dto.setProductCount(productRepository.countByCategoryId(category.getId()));
        return dto;
    }

    private String normalizeDisplayName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }
}
