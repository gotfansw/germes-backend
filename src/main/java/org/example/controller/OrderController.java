package org.example.controller;

import org.example.dto.OrderDTO;
import org.example.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/place/{cartId}")
    public OrderDTO placeOrder(@PathVariable Long cartId) {
        return orderService.placeOrder(cartId);
    }

    @GetMapping
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }
}
