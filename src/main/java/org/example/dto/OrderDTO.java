package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {
    private Long id;
    private LocalDateTime createdAt;
    private double totalPrice;
    private List<OrderItemDTO> items;

    public OrderDTO(Long id, LocalDateTime createdAt, double totalPrice, List<OrderItemDTO> items) {
        this.id = id;
        this.createdAt = createdAt;
        this.totalPrice = totalPrice;
        this.items = items;
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public double getTotalPrice() { return totalPrice; }
    public List<OrderItemDTO> getItems() { return items; }

    public static class OrderItemDTO {
        private Long id;
        private String productName;
        private double price;
        private int quantity;

        public OrderItemDTO(Long id, String productName, double price, int quantity) {
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
