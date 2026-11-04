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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody ProductDto productDto,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.getUserFromJwt(jwt);

        // Auto-set storeId from logged-in user if not provided
        if (user != null && user.getStore() != null && productDto.getStoreId() == null) {
            productDto.setStoreId(user.getStore().getId());
        }

        return ResponseEntity.ok(productService.createProduct(productDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.updateProduct(id, productDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(
            @PathVariable Long id) {
        productService.deleteProduct(id);

        return ResponseEntity.ok(new ApiResponse("Product deleted successfully"));
    }

    @GetMapping("/storeId/{storeId}")
    public ResponseEntity<List<ProductDto>> getProductsByStoreId(
            @PathVariable Long storeId) {
        return ResponseEntity.ok(productService.getProductsById(storeId));
    }

    @GetMapping("/storeId/{storeId}/paged")
    public ResponseEntity<Page<ProductDto>> getProductsPaged(
            @PathVariable Long storeId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getProductsPaged(storeId, search, pageable));
    }

    @GetMapping("/search/{storeId}/{keyword}")
    public ResponseEntity<List<ProductDto>> searchByKeyword(
            @PathVariable Long storeId,
            @PathVariable String keyword) {
        return ResponseEntity.ok(productService.searchByKeyword(storeId, keyword));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.getUserFromJwt(jwt);
        if (user.getStore() == null) {
            throw new org.springframework.security.access.AccessDeniedException("No store is assigned to this account");
        }
        Long storeId = user.getStore().getId();

        return ResponseEntity.ok(productService.getAllProducts(storeId));
    }

    // Public endpoint for testing
    @GetMapping("/public/all")
    public ResponseEntity<List<ProductDto>> getAllProductsPublic() {
        return ResponseEntity.ok(productService.getAllProductsWithoutStoreFilter());
    }
}
