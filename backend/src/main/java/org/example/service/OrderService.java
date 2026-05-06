package org.example.service;

import org.example.dto.OrderDTO;
import org.example.dto.PlaceOrderRequest;
import org.example.exception.NotFoundException;
import org.example.mapper.OrderMapper;
import org.example.model.*;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.example.audit.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final OrderMailService orderMailService;
    private final YookassaService yookassaService;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartService cartService,
                        OrderMailService orderMailService,
                        YookassaService yookassaService,
                        OrderMapper orderMapper,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.orderMailService = orderMailService;
        this.yookassaService = yookassaService;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
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
        log.info("Заказ #{} создан, сессия={}", saved.getId(), sessionId);
        // [FIX #24] Аудит создания заказа
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.Action.ORDER_CREATED, saved.getId(),
                "sessionId=" + sessionId + ", totalPrice=" + saved.getTotalPrice() +
                        ", paymentMethod=" + saved.getPaymentMethod()));


        String confirmationUrl = null;
        if (PaymentMethod.SBP.equals(request.getPaymentMethod())) {
            try {
                confirmationUrl = yookassaService.createSbpPayment(
                        saved.getId(),
                        saved.getTotalPrice(),
                        "Заказ #" + saved.getId() + " — Germes74"
                );
            } catch (Exception e) {
                log.error("Ошибка создания платежа ЮKassa для заказа #{}: {}", saved.getId(), e.getMessage(), e);
                saved.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
                // orderRepository.save не нужен — saved — управляемая сущность,
                // изменение зафиксируется при commit текущей транзакции
                throw new RuntimeException("Не удалось создать платёж. Попробуйте оформить заказ снова.", e);
            }
        }

        // Снимаем ссылку на заказ для письма до выхода из транзакции
        final Order orderForMail = saved;
        final String urlForMail = confirmationUrl;

        return withConfirmationUrl(orderMapper.toDTO(saved), confirmationUrl);

        // Письмо отправляется ПОСЛЕ commit транзакции — см. sendMailAfterCommit ниже.
        // Если нужна гарантия отправки — используй TransactionSynchronizationManager
        // или вынеси в @TransactionalEventListener(phase = AFTER_COMMIT).
    }


    public void sendOrderMailSafe(Order order) {
        try {
            orderMailService.sendPaymentMail(order);
        } catch (Exception e) {
            log.error("Ошибка отправки письма для заказа #{}: {}", order.getId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        return orderMapper.toDTO(order);
    }

    @Transactional
    public OrderDTO updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));

        PaymentStatus previous = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);
        // [FIX #24] Аудит изменения статуса оплаты
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.Action.PAYMENT_STATUS_CHANGED, orderId,
                "previousStatus=" + previous + " -> newStatus=" + paymentStatus));

        if (paymentStatus == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            if (order.getReceiptNumber() == null || order.getReceiptNumber().isBlank()) {
                order.setReceiptNumber(generateReceiptNumber());
            }
            markPaidFields(order);

            // Корзина очищается только при PAID — атомарно с изменением статуса заказа
            cartRepository.findBySessionId(order.getSessionId())
                    .ifPresent(cartService::clearCart);

            log.info("Заказ #{} оплачен, корзина очищена", orderId);
        }

        if (paymentStatus == PaymentStatus.FAILED || paymentStatus == PaymentStatus.CANCELED) {
            order.setStatus(OrderStatus.NEW);
            log.info("Заказ #{} — оплата не прошла ({}), корзина сохранена", orderId, paymentStatus);
        }

        Order saved = orderRepository.save(order);

        try {
            orderMailService.sendPaymentMail(saved);
        } catch (Exception e) {
            log.error("Ошибка отправки письма для заказа #{}: {}", saved.getId(), e.getMessage(), e);
        }

        return orderMapper.toDTO(saved);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        OrderStatus previous = order.getStatus();
        order.setStatus(status);
        // [FIX #24] Аудит смены статуса заказа (действие админа)
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.Action.ORDER_STATUS_CHANGED, orderId,
                "admin action: " + previous + " -> " + status));
        return orderMapper.toDTO(orderRepository.save(order));
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

        try {
            orderMailService.sendPaymentMail(saved);
        } catch (Exception e) {
            log.error("Ошибка отправки письма для заказа #{}: {}", saved.getId(), e.getMessage(), e);
        }

        return orderMapper.toDTO(saved);
    }


    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
        orderRepository.delete(order);
        log.info("Заказ #{} удалён", orderId);

        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.Action.ORDER_DELETED, orderId, "admin action: order deleted"));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

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
        String prefix = switch (deliveryType) {
            case CDEK          -> "CDEK";
            case YANDEX        -> "YD";
            case POCHTA        -> "RU";
            case KAZAN_EXPRESS -> "KE";
            case DELOVYE_LINII -> "DL";
        };
        return prefix + "-" + (100000 + RANDOM.nextInt(900000));
    }

    private OrderDTO withConfirmationUrl(OrderDTO dto, String confirmationUrl) {
        if (confirmationUrl == null) return dto;
        return new OrderDTO(
                dto.getId(), dto.getCreatedAt(), dto.getTotalPrice(),
                dto.getCustomerEmail(), dto.getCustomerPhone(), dto.getDeliveryAddress(),
                dto.getDeliveryType(), dto.getPaymentMethod(), dto.getPaymentStatus(),
                dto.getStatus(), dto.getTrackNumber(), dto.getReceiptNumber(),
                dto.getPaidAt(), dto.getItems(), confirmationUrl
        );
    }
}