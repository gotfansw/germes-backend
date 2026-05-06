package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELED,
    PAYMENT_FAILED;

    @JsonCreator
    public static PaymentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentStatus не может быть пустым");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестный статус оплаты: '" + value + "'. " +
                            "Допустимые значения: PENDING, PAID, FAILED, CANCELED, PAYMENT_FAILED"
            );
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}