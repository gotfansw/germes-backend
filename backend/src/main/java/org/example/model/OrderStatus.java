package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    NEW,
    PAID,
    SHIPPED,
    DELIVERED;

    @JsonCreator
    public static OrderStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderStatus не может быть пустым");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестный статус заказа: '" + value + "'. " +
                            "Допустимые значения: NEW, PAID, SHIPPED, DELIVERED"
            );
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}