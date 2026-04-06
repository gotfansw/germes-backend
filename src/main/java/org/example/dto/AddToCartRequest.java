package org.example.dto;

public class AddToCartRequest {
    private Long cartId;      // null — создать новую корзину, не null — добавить в существующую
    private Long productId;
    private int quantity;

    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
