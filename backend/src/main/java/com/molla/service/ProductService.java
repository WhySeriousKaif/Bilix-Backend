package com.molla.service;

import com.molla.model.User;
import com.molla.payload.dto.ProductDto;

import java.util.List;

public interface ProductService {

    ProductDto createProduct(ProductDto productDto, User user) ;
    ProductDto updateProduct(Long id, ProductDto productDto,User user) ;
    void deleteProduct(Long id,User user);
    List<ProductDto>getProductsById(Long storeId) ;
    List<ProductDto>searchByKeyword(Long storeId,String keyword) ;
    List<ProductDto>getAllProducts(Long storeId) ;
    List<ProductDto>getAllProductsWithoutStoreFilter() ; // For testing - get all products

}
