package org.example.mapper;

import org.example.dto.CartDTO;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "totalPrice", expression = "java(cart.getTotalPrice())")
    CartDTO toDTO(Cart cart);

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "productId", source = "product.id")
    CartDTO.CartItemDTO toItemDTO(CartItem item);
}