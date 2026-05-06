package org.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * [FIX #19] Добавлены:
 * - unique constraint на name — дублирование продуктов невозможно на уровне БД
 *   (DataLoader проверяет existsByName, но без constraint возможна гонка)
 * - индекс на category_id — запросы "продукты по категории" без full scan
 */
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_products_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_products_category_id", columnList = "category_id")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}