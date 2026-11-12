package com.molla.service.impl;

import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Category;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.mapper.ProductMapper;
import com.molla.payload.dto.ProductDto;
import com.molla.repository.CategoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.ProductService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {
    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "all", "and", "category", "find", "for", "from",
            "in", "of", "product", "products", "search", "store", "the"
    );

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll", "categoriesByStore" }, allEntries = true)
    public ProductDto createProduct(ProductDto productDto) {
        if (productDto.getStoreId() == null) {
            throw new BadRequestException("You need to create a store before adding products.");
        }
        if (productDto.getCategoryId() == null) {
            throw new BadRequestException("You need to create a category before adding products.");
        }
        Store store = storeRepository.findById(
                productDto.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + productDto.getStoreId()));
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + productDto.getCategoryId()));
        if (category.getStore() == null || !category.getStore().getId().equals(store.getId())) {
            throw new BadRequestException("Selected category does not belong to this store");
        }
        accessControlService.requireStoreAccess(userService.getCurrentUser(), store.getId());

        // Check if SKU already exists
        if (productDto.getSku() != null && !productDto.getSku().isEmpty()) {
            productRepository.findBySku(productDto.getSku())
                    .ifPresent(existingProduct -> {
                        throw new BadRequestException("Product with SKU '" + productDto.getSku() + "' already exists");
                    });
        }

        Product product = ProductMapper.toEntity(productDto, store, category);

        try {
            Product savedProduct = productRepository.save(product);
            return ProductMapper.toDto(savedProduct);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new BadRequestException("Product with SKU '" + productDto.getSku() + "' already exists");
            }
            throw new BadRequestException("Failed to create product: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll", "categoriesByStore" }, allEntries = true)
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setMrp(productDto.getMrp());
        product.setSellingPrice(productDto.getSellingPrice());
        // Update price to match sellingPrice if price column exists in database
        if (productDto.getSellingPrice() != null) {
            product.setPrice(productDto.getSellingPrice());
        }
        // Update quantity if provided
        if (productDto.getQuantity() != null) {
            product.setQuantity(productDto.getQuantity());
        }
        product.setBrand(productDto.getBrand());
        product.setImage(productDto.getImage());
        if (productDto.getStoreId() != null) {
            Store store = storeRepository.findById(productDto.getStoreId())
                    .orElseThrow(() -> new NotFoundException("Store not found with id: " + productDto.getStoreId()));
            product.setStore(store);
        }
        if (productDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + productDto.getCategoryId()));
            if (product.getStore() == null || category.getStore() == null
                    || !category.getStore().getId().equals(product.getStore().getId())) {
                throw new BadRequestException("Selected category does not belong to this store");
            }
            product.setCategory(category);
        }
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDto(savedProduct);
    }

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll", "categoriesByStore" }, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        productRepository.delete(product);

    }

    @Override
    @Cacheable(cacheNames = "productsByStore", key = "#storeId")
    public List<ProductDto> getProductsById(Long storeId) {
        // Any authenticated user (cashier, branch manager, etc.) can pass any storeId here —
        // without this check, a cashier from Store A could read Store B's whole catalog just
        // by changing the id in the request.
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        List<Product> products = productRepository.findByStoreId(storeId);
        return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<ProductDto> getProductsPaged(Long storeId, Pageable pageable) {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        return productRepository.findByStoreId(storeId, pageable).map(ProductMapper::toDto);
    }

    @Override
    public Page<ProductDto> getProductsPaged(Long storeId, String search, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return productRepository.findByStoreIdFiltered(storeId, normalizedSearch, pageable)
                .map(ProductMapper::toDto);
    }

    @Override
    public List<ProductDto> searchByKeyword(Long storeId, String keyword) {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        List<String> tokens = List.of(normalized.split("[^a-z0-9]+"))
                .stream()
                .filter(token -> token.length() >= 2 && !SEARCH_STOP_WORDS.contains(token))
                .toList();

        List<Product> products;
        if (tokens.isEmpty()) {
            products = productRepository.searchByKeyword(storeId, normalized);
        } else {
            Map<Long, Product> matches = null;
            for (String token : tokens) {
                List<String> alternatives = token.startsWith("electronic")
                        ? List.of(token, "elec")
                        : List.of(token);
                Map<Long, Product> tokenMatches = new LinkedHashMap<>();
                for (String alternative : alternatives) {
                    productRepository.searchByKeyword(storeId, alternative)
                            .forEach(product -> tokenMatches.put(product.getId(), product));
                }
                if (matches == null) {
                    matches = tokenMatches;
                } else {
                    matches.keySet().retainAll(tokenMatches.keySet());
                }
            }
            products = matches == null ? List.of() : List.copyOf(matches.values());
        }
        return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productsByStore", key = "#storeId")
    public List<ProductDto> getAllProducts(Long storeId) {
        List<Product> products = productRepository.findByStoreId(storeId);
        return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productsAll")
    public List<ProductDto> getAllProductsWithoutStoreFilter() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }
}
