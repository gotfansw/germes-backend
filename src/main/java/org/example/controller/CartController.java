package org.example.controller;

import org.example.dto.AddToCartRequest;
import org.example.dto.CartDTO;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.example.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final ProductRepository productRepository;

    public CartController(CartService cartService, ProductRepository productRepository) {
        this.cartService = cartService;
        this.productRepository = productRepository;
    }


    @PostMapping("/add")
    public CartDTO addToCart(@RequestBody AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + request.getProductId()));
        return cartService.addToCart(request.getCartId(), product, request.getQuantity());
    }
}
