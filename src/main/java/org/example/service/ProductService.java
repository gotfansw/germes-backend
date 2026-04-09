package org.example.service;

import org.example.dto.CategoryDTO;
import org.example.dto.ProductDTO;
import org.example.exception.NotFoundException;
import org.example.mapper.ProductMapper;
import org.example.model.Category;
import org.example.model.Product;
import org.example.repository.CategoryRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDTO)
                .toList();
    }

    @Transactional
    public ProductDTO createProduct(String name, BigDecimal price, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + categoryId));
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setCategory(category);
        return productMapper.toDTO(productRepository.save(p));
    }

    @Transactional
    public ProductDTO updateProduct(Long id, String name, BigDecimal price, Long categoryId) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + categoryId));
        p.setName(name);
        p.setPrice(price);
        p.setCategory(category);
        return productMapper.toDTO(productRepository.save(p));
    }

    public CategoryDTO createCategory(String name) {
        Category cat = new Category();
        cat.setName(name);
        return productMapper.toDTO(categoryRepository.save(cat));
    }

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(productMapper::toDTO)
                .toList();
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Категория не найдена: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Товар не найден: " + id);
        }
        productRepository.deleteById(id);
    }
}