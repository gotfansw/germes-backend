package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.CategoryDTO;
import org.example.dto.CreateCategoryRequest;
import org.example.dto.CreateProductRequest;
import org.example.dto.ProductDTO;
import org.example.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // CategoryRepository убран — поиск категории теперь внутри сервиса
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping("/category")
    public CategoryDTO createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return productService.createCategory(request.getName());
    }

    @GetMapping("/categories")
    public List<CategoryDTO> getAllCategories() {
        return productService.getAllCategories();
    }

    @PutMapping("/category/{id}")
    public CategoryDTO updateCategory(@PathVariable Long id,
                                      @Valid @RequestBody CreateCategoryRequest request) {
        return productService.updateCategory(id, request.getName());
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ProductDTO createProduct(@Valid @RequestBody CreateProductRequest request) {
        // categoryId передаём в сервис, не ищем категорию в контроллере
        return productService.createProduct(request.getName(), request.getPrice(), request.getCategoryId());
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable Long id,
                                    @Valid @RequestBody CreateProductRequest request) {
        return productService.updateProduct(id, request.getName(), request.getPrice(), request.getCategoryId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}