package org.example.model;
import com.fasterxml.jackson.annotation.JsonCreator;
public enum DeliveryType {
    DELOVYE_LINII, CDEK;

    @JsonCreator
    public static DeliveryType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}