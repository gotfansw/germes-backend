package org.example.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.dto.AddToCartRequest;
import org.example.dto.CartDTO;
import org.example.dto.UpdateCartItemRequest;
import org.example.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private String resolveSessionId(String headerSession, HttpSession session) {
        return (headerSession != null && !headerSession.isBlank())
                ? headerSession
                : session.getId();
    }

    @PostMapping("/add")
    public CartDTO addToCart(@Valid @RequestBody AddToCartRequest request,
                             @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                             HttpSession session) {
        return cartService.addToCart(resolveSessionId(headerSession, session), request.getProductId(), request.getQuantity());
    }

    @GetMapping
    public CartDTO getCart(@RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                           HttpSession session) {
        return cartService.getCart(resolveSessionId(headerSession, session));
    }

    @PatchMapping("/item/{itemId}")
    public CartDTO updateCartItem(@PathVariable Long itemId,
                                  @Valid @RequestBody UpdateCartItemRequest request,
                                  @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                                  HttpSession session) {
        return cartService.updateCartItem(resolveSessionId(headerSession, session), itemId, request.getQuantity());
    }

    @DeleteMapping("/item/{itemId}")
    public CartDTO removeFromCart(@PathVariable Long itemId,
                                  @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                                  HttpSession session) {
        return cartService.removeFromCart(resolveSessionId(headerSession, session), itemId);
    }

    @DeleteMapping("/clear")
    public CartDTO clearCart(@RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                             HttpSession session) {
        return cartService.clearCart(resolveSessionId(headerSession, session));
    }
}
