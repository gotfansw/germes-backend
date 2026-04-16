package org.example.service;

import org.example.dto.OrderDTO;
import org.example.dto.PlaceOrderRequest;
import org.example.exception.NotFoundException;
import org.example.model.*;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final OrderMailService orderMailService;
    private final YookassaService yookassaService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartService cartService,
                        OrderMailService orderMailService,
                        YookassaService yookassaService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.orderMailService = orderMailService;
        this.yookassaService = yookassaService;
    }

    @Transactional
    public OrderDTO placeOrder(String sessionId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Корзина не найдена для сессии"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Нельзя оформить заказ: корзина пуста");
        }

        Order order = new Order();
        order.setSessionId(sessionId);
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryType(request.getDeliveryType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.NEW);
        order.setReceiptNumber(generateReceiptNumber());

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

        // Создаём платёж в ЮКассе и сохраняем URL подтверждения в DTO
        String confirmationUrl = null;
        if (PaymentMethod.SBP.equals(request.getPaymentMethod())) {
            try {
                confirmationUrl = yookassaService.createSbpPayment(
                        saved.getId(),
                        saved.getTotalPrice(),
                        "Заказ #" + saved.getId() + " — Germes74"
                );
            } catch (Exception e) {
                // Логируем, но не падаем — заказ уже создан
                e.printStackTrace();
            }
        }

        orderMailService.sendPaymentMail(saved);
        return toDTO(saved, confirmationUrl);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(o -> toDTO(o, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        return toDTO(order, null);
    }

    @Transactional
    public OrderDTO updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));

        order.setPaymentStatus(paymentStatus);

        if (paymentStatus == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            if (order.getReceiptNumber() == null || order.getReceiptNumber().isBlank()) {
                order.setReceiptNumber(generateReceiptNumber());
            }
            markPaidFields(order);
            cartRepository.findBySessionId(order.getSessionId()).ifPresent(cartService::clearCart);
        }

        if (paymentStatus == PaymentStatus.FAILED || paymentStatus == PaymentStatus.CANCELED) {
            order.setStatus(OrderStatus.NEW);
        }

        Order saved = orderRepository.save(order);
        orderMailService.sendPaymentMail(saved);
        return toDTO(saved, null);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        order.setStatus(status);
        return toDTO(orderRepository.save(order), null);
    }

    @Transactional
    public OrderDTO updateTracking(Long orderId, String trackNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        order.setTrackNumber(trackNumber);
        if (order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.SHIPPED);
        }
        Order saved = orderRepository.save(order);
        orderMailService.sendPaymentMail(saved);
        return toDTO(saved, null);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new NotFoundException("Заказ не найден: " + orderId);
        }
        orderRepository.deleteById(orderId);
    }

    private void markPaidFields(Order order) {
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        if (order.getTrackNumber() == null || order.getTrackNumber().isBlank()) {
            order.setTrackNumber(generateTrackNumber(order.getDeliveryType()));
        }
    }

    private String generateReceiptNumber() {
        return "RCPT-" + System.currentTimeMillis();
    }

    private String generateTrackNumber(DeliveryType deliveryType) {
        String prefix = deliveryType == DeliveryType.DELOVYE_LINII ? "DL" : "CDEK";
        return prefix + "-" + (100000 + RANDOM.nextInt(900000));
    }

    private OrderDTO toDTO(Order order, String confirmationUrl) {
        List<OrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(i -> new OrderDTO.OrderItemDTO(
                        i.getId(),
                        i.getProductName(),
                        i.getPrice(),
                        i.getQuantity()))
                .toList();

        return new OrderDTO(
                order.getId(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getDeliveryType(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getStatus(),
                order.getTrackNumber(),
                order.getReceiptNumber(),
                order.getPaidAt(),
                itemDTOs,
                confirmationUrl
        );
    }
}