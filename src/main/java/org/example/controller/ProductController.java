package org.example.controller;

import org.example.model.Category;
import org.example.model.Product;
import org.example.repository.CategoryRepository;
import org.example.repository.ProductRepository;
import org.example.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService,
                             CategoryRepository categoryRepository,
                             ProductRepository productRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PostMapping("/category")
    public Category createCategory(@RequestParam String name) {
        return productService.createCategory(name);
    }

    @PostMapping
    public Product createProduct(@RequestParam String name,
                                 @RequestParam double price,
                                 @RequestParam Long categoryId) {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));
        return productService.createProduct(name, price, cat);
    }
}