package org.example.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartDTO {
    private Long id;
    private List<CartItemDTO> items;
    private BigDecimal totalPrice;

    // Пустой конструктор — нужен для getCart когда корзины ещё нет
    public CartDTO() {
        this.items = new ArrayList<>();
        this.totalPrice = BigDecimal.ZERO;
    }

    public CartDTO(Long id, List<CartItemDTO> items, BigDecimal totalPrice) {
        this.id = id;
        this.items = items != null ? items : new ArrayList<>();
        this.totalPrice = totalPrice != null ? totalPrice : BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items != null ? items : new ArrayList<>(); }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice != null ? totalPrice : BigDecimal.ZERO; }

    public static class CartItemDTO {
        private Long id;
        private String productName;
        private BigDecimal price;
        private int quantity;

        public CartItemDTO() {}

        public CartItemDTO(Long id, String productName, BigDecimal price, int quantity) {
            this.id = id;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}