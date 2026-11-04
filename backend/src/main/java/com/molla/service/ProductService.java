package com.molla.service;

import com.molla.payload.dto.ProductDto;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);
    List<ProductDto>getProductsById(Long storeId) ;
    Page<ProductDto> getProductsPaged(Long storeId, Pageable pageable);
    Page<ProductDto> getProductsPaged(Long storeId, String search, Pageable pageable);
    List<ProductDto>searchByKeyword(Long storeId,String keyword) ;
    List<ProductDto>getAllProducts(Long storeId) ;
    List<ProductDto>getAllProductsWithoutStoreFilter() ; // For testing - get all products

}
