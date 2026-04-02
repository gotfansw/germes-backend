package org.example.service;

import org.example.model.*;
import org.hibernate.Session;
import java.time.LocalDateTime;

public class OrderService {

    private Session session;

    public OrderService(Session session) {
        this.session = session;
    }

    public Order placeOrder(Cart cart) {
        // Создаём новый заказ
        Order order = new Order();
        order.setCreatedAt(LocalDateTime.now());

        // Переносим каждый товар из корзины в заказ
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        // Фиксируем итоговую сумму
        order.setTotalPrice(cart.getTotalPrice());

        // Сохраняем заказ в БД
        session.persist(order);

        return order;
    }
}