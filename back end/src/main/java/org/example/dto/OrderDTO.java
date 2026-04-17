package org.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDTO {

    private Long id;
    private LocalDateTime createdAt;
    private BigDecimal totalPrice;
    private String customerEmail;
    private String customerPhone;       // НОВОЕ
    private String deliveryAddress;     // НОВОЕ
    private DeliveryType deliveryType;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus status;
    private String trackNumber;
    private String receiptNumber;
    private LocalDateTime paidAt;
    private List<OrderItemDTO> items;
    private String confirmationUrl;     // НОВОЕ — ссылка на оплату ЮКассы

    public OrderDTO(Long id, LocalDateTime createdAt, BigDecimal totalPrice,
                    String customerEmail, String customerPhone, String deliveryAddress,
                    DeliveryType deliveryType, PaymentMethod paymentMethod,
                    PaymentStatus paymentStatus, OrderStatus status,
                    String trackNumber, String receiptNumber, LocalDateTime paidAt,
                    List<OrderItemDTO> items, String confirmationUrl) {
        this.id = id;
        this.createdAt = createdAt;
        this.totalPrice = totalPrice;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryType = deliveryType;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.status = status;
        this.trackNumber = trackNumber;
        this.receiptNumber = receiptNumber;
        this.paidAt = paidAt;
        this.items = items;
        this.confirmationUrl = confirmationUrl;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public DeliveryType getDeliveryType() { return deliveryType; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public OrderStatus getStatus() { return status; }
    public String getTrackNumber() { return trackNumber; }
    public String getReceiptNumber() { return receiptNumber; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public List<OrderItemDTO> getItems() { return items; }
    public String getConfirmationUrl() { return confirmationUrl; }

    // ── Inner DTO ────────────────────────────────────────────────────────────

    public record OrderItemDTO(Long id, String productName, BigDecimal price, int quantity) {}
}