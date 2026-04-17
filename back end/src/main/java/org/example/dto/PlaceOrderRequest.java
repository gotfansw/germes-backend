package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.example.model.DeliveryType;
import org.example.model.PaymentMethod;

public class PlaceOrderRequest {

    @Email(message = "Некорректный email")
    @NotBlank(message = "Email обязателен")
    private String customerEmail;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7[0-9]{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")    private String customerPhone;

    @NotBlank(message = "Адрес доставки обязателен")
    private String deliveryAddress;

    @NotNull(message = "Способ доставки обязателен")
    private DeliveryType deliveryType;

    @NotNull(message = "Способ оплаты обязателен")
    private PaymentMethod paymentMethod;

    private boolean paymentConfirmed;

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isPaymentConfirmed() { return paymentConfirmed; }
    public void setPaymentConfirmed(boolean paymentConfirmed) { this.paymentConfirmed = paymentConfirmed; }
}