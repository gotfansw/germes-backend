package org.example.service;

import org.example.dto.OrderDTO;
import org.example.model.*;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
    }

    @Transactional
    public OrderDTO placeOrder(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена: " + cartId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Нельзя оформить заказ: корзина пуста");
        }

        Order order = new Order();
        order.setCreatedAt(LocalDateTime.now());

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        order.setTotalPrice(cart.getTotalPrice());
        Order saved = orderRepository.save(order);


        cartService.clearCart(cart);

        return toDTO(saved);
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private OrderDTO toDTO(Order order) {
        List<OrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(i -> new OrderDTO.OrderItemDTO(
                        i.getId(),
                        i.getProductName(),
                        i.getPrice(),
                        i.getQuantity()))
                .toList();
        return new OrderDTO(order.getId(), order.getCreatedAt(), order.getTotalPrice(), itemDTOs);
    }
}
