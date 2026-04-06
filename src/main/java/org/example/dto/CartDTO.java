package org.example.dto;

import java.util.List;

public class CartDTO {
    private Long id;
    private List<CartItemDTO> items;
    private double totalPrice;

    public CartDTO(Long id, List<CartItemDTO> items, double totalPrice) {
        this.id = id;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public Long getId() { return id; }
    public List<CartItemDTO> getItems() { return items; }
    public double getTotalPrice() { return totalPrice; }

    public static class CartItemDTO {
        private Long id;
        private String productName;
        private double price;
        private int quantity;

        public CartItemDTO(Long id, String productName, double price, int quantity) {
            this.id = id;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
        }

        public Long getId() { return id; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }
}
