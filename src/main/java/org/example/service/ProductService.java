package org.example.service;

import org.example.model.Category;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.hibernate.Session;

public class ProductService {

    private ProductRepository productRepository;

    public ProductService(Session session) {
        this.productRepository = new ProductRepository(session);
    }

    public Product createProduct(String name, double price, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setCategory(category);
        productRepository.saveProduct(p);
        return p;
    }

    public Category createCategory(String name) {
        Category cat = new Category();
        cat.setName(name);
        productRepository.saveCategory(cat);
        return cat;
    }
}