package org.example.dto;

import jakarta.validation.constraints.NotNull;
import org.example.model.PaymentStatus;

public class UpdatePaymentStatusRequest {

    @NotNull(message = "Статус оплаты обязателен")
    private PaymentStatus paymentStatus;

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
}
