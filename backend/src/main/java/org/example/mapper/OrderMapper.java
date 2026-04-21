package org.example.mapper;

import org.example.dto.OrderDTO;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "confirmationUrl", ignore = true)
    OrderDTO toDTO(Order order);

    OrderDTO.OrderItemDTO toItemDTO(OrderItem item);
}