package org.example.dto;

import org.example.model.PaymentStatus;

public class PaymentStatusResponse {
    private Long orderId;
    private PaymentStatus paymentStatus;
    private String receiptNumber;
    private String trackNumber;

    public PaymentStatusResponse(Long orderId, PaymentStatus paymentStatus, String receiptNumber, String trackNumber) {
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.receiptNumber = receiptNumber;
        this.trackNumber = trackNumber;
    }

    public Long getOrderId() { return orderId; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getReceiptNumber() { return receiptNumber; }
    public String getTrackNumber() { return trackNumber; }
}
