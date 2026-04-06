package org.example.dto;

public class ProductDTO {
    private Long id;
    private String name;
    private double price;
    private String categoryName;

    public ProductDTO(Long id, String name, double price, String categoryName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.categoryName = categoryName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategoryName() { return categoryName; }
}
