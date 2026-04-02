package org.example.service;

import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.hibernate.Session;

public class CartService {

    private CartRepository cartRepository;

    public CartService(Session session) {
        this.cartRepository = new CartRepository(session);
    }

    public Cart addToCart(Product product, int quantity) {
        Cart cart = new Cart();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setCart(cart);

        cart.getItems().add(item);
        cartRepository.saveCart(cart);

        return cart;
    }
}