package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryType {
    CDEK,
    YANDEX,
    POCHTA,
    KAZAN_EXPRESS,
    DELOVYE_LINII;

    @JsonCreator
    public static DeliveryType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeliveryType не может быть пустым");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестный тип доставки: '" + value + "'. " +
                            "Допустимые значения: CDEK, YANDEX, POCHTA, KAZAN_EXPRESS, DELOVYE_LINII"
            );
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}