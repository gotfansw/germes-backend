package org.example.service;

import org.example.dto.CartDTO;
import org.example.exception.NotFoundException;
import org.example.mapper.CartMapper;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setSessionId(sessionId);
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public CartDTO addToCart(String sessionId, Long productId, int quantity) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });

        cart = cartRepository.findByIdWithLock(cart.getId())
                .orElseThrow(() -> new NotFoundException("Корзина не найдена"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + productId));

        Cart finalCart = cart;
        cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> {
                            CartItem item = new CartItem();
                            item.setProduct(product);
                            item.setQuantity(quantity);
                            item.setCart(finalCart);
                            finalCart.getItems().add(item);
                        }
                );

        return cartMapper.toDTO(cartRepository.save(cart));
    }

    // Возвращает пустую корзину если сессия новая — не бросает 404
    @Transactional(readOnly = true)
    public CartDTO getCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .map(cartMapper::toDTO)
                .orElseGet(CartDTO::new);
    }

    @Transactional
    public CartDTO removeFromCart(String sessionId, Long itemId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Корзина не найдена"));
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        return cartMapper.toDTO(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}