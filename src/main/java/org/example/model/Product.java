package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;

    // Многие к одному: много продуктов относятся к одной категории
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Product() {}

    // Геттеры и сеттеры (Alt + Insert)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public void setCategory(Category category) { this.category = category; }
}
