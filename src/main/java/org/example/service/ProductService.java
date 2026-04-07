package org.example.service;

import org.example.dto.CategoryDTO;
import org.example.dto.ProductDTO;
import org.example.model.Category;
import org.example.model.Product;
import org.example.repository.CategoryRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductDTO createProduct(String name, double price, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setCategory(category);
        return toDTO(productRepository.save(p));
    }

    public CategoryDTO createCategory(String name) {
        Category cat = new Category();
        cat.setName(name);
        Category saved = categoryRepository.save(cat);
        return new CategoryDTO(saved.getId(), saved.getName());
    }

    // --- НОВЫЕ МЕТОДЫ ---

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(c.getId(), c.getName()))
                .toList();
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public ProductDTO updateProduct(Long id, String name, double price, Category category) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + id));
        p.setName(name);
        p.setPrice(price);
        p.setCategory(category);
        return toDTO(productRepository.save(p));
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO toDTO(Product p) {
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        return new ProductDTO(p.getId(), p.getName(), p.getPrice(), categoryName);
    }
}