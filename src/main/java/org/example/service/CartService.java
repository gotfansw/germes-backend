package org.example.service;

import org.example.dto.CartDTO;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public CartDTO addToCart(Long cartId, Product product, int quantity) {
        Cart cart;

        if (cartId == null) {

            cart = new Cart();
        } else {

            cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new RuntimeException("Корзина не найдена: " + cartId));
        }

        // Если товар уже есть в корзине — увеличиваем количество
        cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> {
                            CartItem item = new CartItem();
                            item.setProduct(product);
                            item.setQuantity(quantity);
                            item.setCart(cart);
                            cart.getItems().add(item);
                        }
                );

        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }


    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public CartDTO toDTO(Cart cart) {
        List<CartDTO.CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(i -> new CartDTO.CartItemDTO(
                        i.getId(),
                        i.getProduct().getName(),
                        i.getProduct().getPrice(),
                        i.getQuantity()))
                .toList();
        return new CartDTO(cart.getId(), itemDTOs, cart.getTotalPrice());
    }
}
