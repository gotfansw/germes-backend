package org.example.service;

import org.example.dto.CartDTO;
import org.example.exception.NotFoundException;
import org.example.mapper.CartMapper;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

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
                    try {
                        Cart cart = new Cart();
                        cart.setSessionId(sessionId);
                        return cartRepository.saveAndFlush(cart); // flush сразу — чтобы constraint сработал здесь
                    } catch (DataIntegrityViolationException e) {
                        // Параллельный запрос уже создал корзину — просто берём её
                        log.debug("Cart already created by concurrent request for session {}, fetching existing", sessionId);
                        return cartRepository.findBySessionId(sessionId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Cart not found after constraint violation for session: " + sessionId));
                    }
                });
    }


    @Transactional
    public CartDTO addToCart(String sessionId, Long productId, int quantity) {
        try {
            Cart cart = getOrCreateCart(sessionId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Товар не найден: " + productId));

            cart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(productId))
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

            return cartMapper.toDTO(cartRepository.save(cart));

        } catch (ObjectOptimisticLockingFailureException e) {
            // Конкурентное изменение корзины — клиент должен повторить запрос
            throw new IllegalStateException("Корзина была изменена параллельным запросом, попробуйте ещё раз", e);
        }
    }

    @Transactional(readOnly = true)
    public CartDTO getCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .map(cartMapper::toDTO)
                .orElseGet(CartDTO::new);
    }

    @Transactional
    public CartDTO updateCartItem(String sessionId, Long itemId, int quantity) {
        try {
            Cart cart = cartRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new NotFoundException("Корзина не найдена"));

            CartItem item = cart.getItems().stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Позиция корзины не найдена: " + itemId));

            item.setQuantity(quantity);
            return cartMapper.toDTO(cartRepository.save(cart));

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Корзина была изменена параллельным запросом, попробуйте ещё раз", e);
        }
    }

    @Transactional
    public CartDTO removeFromCart(String sessionId, Long itemId) {
        try {
            Cart cart = cartRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new NotFoundException("Корзина не найдена"));

            cart.getItems().removeIf(item -> item.getId().equals(itemId));
            return cartMapper.toDTO(cartRepository.save(cart));

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Корзина была изменена параллельным запросом, попробуйте ещё раз", e);
        }
    }

    /**
     * Очистка корзины из контроллера (по sessionId) — возвращает пустой CartDTO.
     */
    @Transactional
    public CartDTO clearCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
        cart.getItems().clear();
        return cartMapper.toDTO(cart);
        // save не нужен: cart — управляемая сущность, изменения применятся при commit
    }


    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
    }
}