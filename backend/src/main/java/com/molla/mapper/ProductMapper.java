package com.molla.mapper;

import com.molla.model.Category;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.payload.dto.ProductDto;

public class ProductMapper {
    
    public static ProductDto toDto(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setSku(product.getSku());
        productDto.setMrp(product.getMrp());
        productDto.setSellingPrice(product.getSellingPrice());
        productDto.setBrand(product.getBrand());
        productDto.setQuantity(product.getQuantity());
        if(product.getCategory() != null) {
            productDto.setCategory(CategoryMapper.toDto(product.getCategory()));
        }
        productDto.setStoreId(product.getStore() != null ? product.getStore().getId() : null);
        productDto.setImage(product.getImage());
        productDto.setCreatedAt(product.getCreatedAt());
        productDto.setUpdatedAt(product.getUpdatedAt());
        return productDto;
    }
    
    public static Product toEntity(ProductDto productDto, Store store, Category category) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setMrp(productDto.getMrp());
        product.setSellingPrice(productDto.getSellingPrice());
        // Set price to sellingPrice if price column exists in database
        product.setPrice(productDto.getSellingPrice());
        // Set quantity, default to 0 if not provided
        product.setQuantity(productDto.getQuantity() != null ? productDto.getQuantity() : 0);
        product.setBrand(productDto.getBrand());
        product.setImage(productDto.getImage());
        product.setStore(store);
        product.setCategory(category);
        return product;
    }
}
