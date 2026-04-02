package org.example;

import org.example.model.*;
import org.example.service.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class App {

    public static void main(String[] args) {
        try {
            SessionFactory factory = new Configuration().configure().buildSessionFactory();
            Session session = factory.openSession();

            session.beginTransaction();

            ProductService productService = new ProductService(session);
            CartService cartService = new CartService(session);
            OrderService orderService = new OrderService(session);

            Category cat = productService.createCategory("Грузики");
            Product p = productService.createProduct("Груза на литые диски", 150.0, cat);

            Cart cart = cartService.addToCart(p, 2);

            Order order = orderService.placeOrder(cart);

            session.getTransaction().commit();

            System.out.println("Корзина создана! ID: " + cart.getId());
            System.out.println("Итого: " + cart.getTotalPrice() + " руб.");
            System.out.println("Заказ оформлен! ID: " + order.getId());
            System.out.println("Сумма заказа: " + order.getTotalPrice() + " руб.");

            session.close();
            factory.close();

        } catch (Exception e) {
            System.err.println("Ошибка!");
            e.printStackTrace();
        }
    }
}