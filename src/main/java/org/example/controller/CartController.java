package org.example.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.dto.AddToCartRequest;
import org.example.dto.CartDTO;
import org.example.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public CartDTO addToCart(@Valid @RequestBody AddToCartRequest request,
                             @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                             HttpSession session) {
        String sessionId = (headerSession != null && !headerSession.isBlank())
                ? headerSession
                : session.getId();
        return cartService.addToCart(sessionId, request.getProductId(), request.getQuantity());
    }

    @GetMapping
    public CartDTO getCart(@RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                           HttpSession session) {
        String sessionId = (headerSession != null && !headerSession.isBlank())
                ? headerSession
                : session.getId();
        return cartService.getCart(sessionId);
    }

    @DeleteMapping("/item/{itemId}")
    public CartDTO removeFromCart(@PathVariable Long itemId,
                                  @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                                  HttpSession session) {
        String sessionId = (headerSession != null && !headerSession.isBlank())
                ? headerSession
                : session.getId();
        return cartService.removeFromCart(sessionId, itemId);
    }
}