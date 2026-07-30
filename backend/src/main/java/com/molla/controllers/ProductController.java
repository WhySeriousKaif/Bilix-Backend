package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.dto.ProductDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.ProductService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody ProductDto productDto,
            @RequestHeader(value = "Authorization", required = false) String jwt
    ) throws UserException {

        User user = null;

        if (jwt != null && !jwt.isEmpty()) {
            try {
                user = userService.getUserFromJwt(jwt);
            } catch (Exception e) {
                // Invalid JWT → continue for testing
            }
        }

        // Auto-set storeId from logged-in user if not provided
        if (user != null && user.getStore() != null && productDto.getStoreId() == null) {
            productDto.setStoreId(user.getStore().getId());
        }

        ProductDto createdProduct = productService.createProduct(productDto, user);
        return ResponseEntity.ok(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto productDto,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(productService.updateProduct(id, productDto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.getUserFromJwt(jwt);
        productService.deleteProduct(id, user);

        return ResponseEntity.ok(new ApiResponse("Product deleted successfully"));
    }

    @GetMapping("/storeId/{storeId}")
    public ResponseEntity<List<ProductDto>> getProductsByStoreId(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        return ResponseEntity.ok(productService.getProductsById(storeId));
    }

    @GetMapping("/search/{storeId}/{keyword}")
    public ResponseEntity<List<ProductDto>> searchByKeyword(
            @PathVariable Long storeId,
            @PathVariable String keyword,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        return ResponseEntity.ok(productService.searchByKeyword(storeId, keyword));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestHeader(value = "Authorization", required = false) String jwt
    ) throws UserException {

        Long storeId = 1L;

        if (jwt != null && !jwt.isEmpty()) {
            try {
                User user = userService.getUserFromJwt(jwt);
                storeId = user.getStore().getId();
            } catch (Exception e) {
                storeId = 1L;
            }
        }

        return ResponseEntity.ok(productService.getAllProducts(storeId));
    }

    // Public endpoint for testing
    @GetMapping("/public/all")
    public ResponseEntity<List<ProductDto>> getAllProductsPublic() {
        return ResponseEntity.ok(productService.getAllProductsWithoutStoreFilter());
    }
}