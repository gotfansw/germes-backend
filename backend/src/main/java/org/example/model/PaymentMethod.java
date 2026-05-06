package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum PaymentMethod {
    SBP;

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentMethod не может быть пустым");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестный метод оплаты: '" + value + "'. " +
                            "Допустимые значения: SBP"
            );
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}