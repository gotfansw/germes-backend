package org.example.controller;

import org.example.dto.CategoryDTO;
import org.example.dto.CreateCategoryRequest;
import org.example.dto.CreateProductRequest;
import org.example.dto.ProductDTO;
import org.example.model.Category;
import org.example.repository.CategoryRepository;
import org.example.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductService productService,
                             CategoryRepository categoryRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }


    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping("/category")
    public CategoryDTO createCategory(@RequestBody CreateCategoryRequest request) {
        return productService.createCategory(request.getName());
    }


    @PostMapping
    public ProductDTO createProduct(@RequestBody CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Категория не найдена: " + request.getCategoryId()));
        return productService.createProduct(request.getName(), request.getPrice(), category);
    }
}
