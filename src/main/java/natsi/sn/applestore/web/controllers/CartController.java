package natsi.sn.applestore.web.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.Cart;
import natsi.sn.applestore.data.models.CartItem;
import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.CartItemRepository;
import natsi.sn.applestore.data.repository.CartRepository;
import natsi.sn.applestore.data.repository.ProductRepository;
import natsi.sn.applestore.web.dto.request.AddToCartRequest;
import natsi.sn.applestore.web.dto.request.UpdateCartItemRequest;
import natsi.sn.applestore.web.dto.response.CartItemResponse;
import natsi.sn.applestore.web.dto.response.CartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        if (authentication == null) {
            CartResponse emptyCart = CartResponse.builder()
                    .items(Collections.emptyList())
                    .total(0.0)
                    .itemCount(0)
                    .build();
            return ResponseEntity.ok(emptyCart);
        }

        User user = (User) authentication.getPrincipal();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId()).orElse(null);

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        return ResponseEntity.ok(convertToDto(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @RequestBody AddToCartRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(403).build();
        }

        User user = (User) authentication.getPrincipal();
        Product product = productRepository.findById(Integer.parseInt(request.getProductId()))
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        CartItem item;
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            item = cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            if (request.getColorId() != null) {
                // TODO: Récupérer ProductColor depuis repository
            }
            if (request.getStorageId() != null) {
                // TODO: Récupérer ProductStorage depuis repository
            }
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(product.getPrice());
            item = cartItemRepository.save(newItem);
        }

        cart.calculateTotals();
        cartRepository.save(cart);

        return ResponseEntity.ok(convertToDto(item));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorisé");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
            Cart cart = item.getCart();
            cart.calculateTotals();
            cartRepository.save(cart);
            return ResponseEntity.ok().build();
        }

        item.setQuantity(request.getQuantity());
        item = cartItemRepository.save(item);

        Cart cart = item.getCart();
        cart.calculateTotals();
        cartRepository.save(cart);

        return ResponseEntity.ok(convertToDto(item));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long itemId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorisé");
        }

        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        cart.calculateTotals();
        cartRepository.save(cart);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        cartItemRepository.deleteByCartId(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartItemCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(new CartCountResponse(0));
        }
        User user = (User) authentication.getPrincipal();
        Integer count = cartItemRepository.sumQuantityByUserId(user.getId());
        return ResponseEntity.ok(new CartCountResponse(count != null ? count : 0));
    }

    private CartResponse convertToDto(Cart cart) {
        List<CartItemResponse> itemDtos = cart.getItems().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemDtos)
                .total(cart.getTotalPrice())
                .itemCount(cart.getTotalItems())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse convertToDto(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .cartId(item.getCart().getId())
                .productId(item.getProductId())
                .colorId(item.getColor() != null ? item.getColor().getId() : null)
                .storageId(item.getStorage() != null ? item.getStorage().getId() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice() * item.getQuantity())
                .addedAt(item.getAddedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    @Data
    public static class CartCountResponse {
        private Integer count;

        public CartCountResponse(Integer count) {
            this.count = count;
        }
    }
}


