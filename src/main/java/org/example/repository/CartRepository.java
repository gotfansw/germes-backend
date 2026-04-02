package org.example.repository;

import org.example.model.Cart;
import org.hibernate.Session;

public class CartRepository {

    private Session session;

    public CartRepository(Session session) {
        this.session = session;
    }

    public void saveCart(Cart cart) {
        session.persist(cart);
    }
}