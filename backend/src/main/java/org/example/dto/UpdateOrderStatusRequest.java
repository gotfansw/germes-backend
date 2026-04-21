package org.example.dto;

import jakarta.validation.constraints.NotNull;
import org.example.model.OrderStatus;

public class UpdateOrderStatusRequest {

    @NotNull(message = "Статус обязателен")
    private OrderStatus status;

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
