package com.molla.service.impl;

import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Category;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.mapper.ProductMapper;
import com.molla.payload.dto.ProductDto;
import com.molla.repository.CategoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll" }, allEntries = true)
    public ProductDto createProduct(ProductDto productDto, User user) {
        Store store = storeRepository.findById(
                productDto.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + productDto.getStoreId()));
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + productDto.getCategoryId()));

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
    @CacheEvict(cacheNames = { "productsByStore", "productsAll" }, allEntries = true)
    public ProductDto updateProduct(Long id, ProductDto productDto, User user) {
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
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDto(savedProduct);
    }

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll" }, allEntries = true)
    public void deleteProduct(Long id, User user) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        productRepository.delete(product);

    }

    @Override
    @Cacheable(cacheNames = "productsByStore", key = "#storeId")
    public List<ProductDto> getProductsById(Long storeId) {
        List<Product> products = productRepository.findByStoreId(storeId);
        return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> searchByKeyword(Long storeId, String keyword) {
        List<Product> products = productRepository.searchByKeyword(storeId, keyword);
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
