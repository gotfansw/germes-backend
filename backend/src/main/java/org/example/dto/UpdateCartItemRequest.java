package org.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateCartItemRequest {


    @NotNull(message = "Количество обязательно")
    @Min(value = 1, message = "Количество должно быть не меньше 1")
    @Max(value = 999, message = "Количество не может превышать 999")
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}