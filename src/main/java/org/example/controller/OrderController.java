package org.example.controller;

import org.example.dto.OrderDTO;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @PostMapping("/place")
    public OrderDTO placeOrder(HttpSession session) {
        // cartId больше не передаётся в URL — берём корзину по сессии
        String sessionId = session.getId();
        return orderService.placeOrder(sessionId);
    }

    @GetMapping
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }
}