package natsi.sn.applestore.web.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import natsi.sn.applestore.data.models.Cart;
import natsi.sn.applestore.data.models.CartItem;
import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.CartItemRepository;
import natsi.sn.applestore.data.repository.CartRepository;
import natsi.sn.applestore.data.repository.ProductRepository;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.ProductColorDto;
import natsi.sn.applestore.web.dto.ProductStorageDto;
import natsi.sn.applestore.web.dto.request.AddToCartRequest;
import natsi.sn.applestore.web.dto.request.UpdateCartItemRequest;
import natsi.sn.applestore.web.dto.response.CartItemResponse;
import natsi.sn.applestore.web.dto.response.CartResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        log.info("🛒 GET /api/cart");

        if (authentication == null) {
            CartResponse emptyCart = CartResponse.builder()
                    .items(Collections.emptyList())
                    .total(0.0)
                    .itemCount(0)
                    .build();
            return ResponseEntity.ok(emptyCart);
        }

        User user = (User) authentication.getPrincipal();
        log.info("✅ Utilisateur: {} (ID: {})", user.getEmail(), user.getId());

        Cart cart = cartRepository.findByUserIdWithItems(user.getId()).orElse(null);

        if (cart == null) {
            log.info("📦 Création d'un nouveau panier");
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        CartResponse response = convertToDto(cart);
        log.info("✅ Panier: {} articles, total: {} F CFA", response.getItemCount(), response.getTotal());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @RequestBody AddToCartRequest request,
            Authentication authentication) {

        log.info("🛒 POST /api/cart/items - productId: {}, quantity: {}",
                request.getProductId(), request.getQuantity());

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        Product product = productRepository.findById(Integer.parseInt(request.getProductId()))
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        log.info("📦 Produit: {} - Prix: {} F CFA", product.getName(), product.getPrice());

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
            log.info("📝 Mise à jour quantité: {} -> {}",
                    existingItem.getQuantity(),
                    existingItem.getQuantity() + request.getQuantity());
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            item = cartItemRepository.save(existingItem);
        } else {
            log.info("➕ Nouvel article");
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

        log.info("✅ Article ajouté. Total: {} F CFA", cart.getTotalPrice());
        return ResponseEntity.ok(convertToDto(item, product));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {

        log.info("📝 PUT /api/cart/items/{} - quantity: {}", itemId, request.getQuantity());

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request.getQuantity() <= 0) {
            log.info("🗑️ Suppression de l'article");
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

        log.info("✅ Article mis à jour");

        Product product = productRepository.findById(Integer.parseInt(item.getProductId())).orElse(null);
        return ResponseEntity.ok(convertToDto(item, product));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long itemId,
            Authentication authentication) {

        log.info("🗑️ DELETE /api/cart/items/{}", itemId);

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        cart.calculateTotals();
        cartRepository.save(cart);

        log.info("✅ Article supprimé");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        log.info("🗑️ DELETE /api/cart (vider)");

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId()).orElse(null);

        if (cart != null) {
            log.info("🗑️ Suppression de {} articles", cart.getItems().size());
            cartItemRepository.deleteByCartId(cart.getId());
            cart.calculateTotals();
            cartRepository.save(cart);
        }

        log.info("✅ Panier vidé");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartItemCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(new CartCountResponse(0));
        }
        User user = (User) authentication.getPrincipal();
        Integer count = cartItemRepository.sumQuantityByUserId(user.getId());
        log.info("📊 Nombre d'articles: {}", count);
        return ResponseEntity.ok(new CartCountResponse(count != null ? count : 0));
    }

    // ✅ Convertir Cart en DTO avec les détails des produits
    private CartResponse convertToDto(Cart cart) {
        List<CartItemResponse> itemDtos = cart.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(Integer.parseInt(item.getProductId())).orElse(null);
                    return convertToDto(item, product);
                })
                .toList();

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

    // ✅ Convertir CartItem en DTO avec Product enrichi
    private CartItemResponse convertToDto(CartItem item, Product product) {
        CartItemResponse.CartItemResponseBuilder builder = CartItemResponse.builder()
                .id(item.getId())
                .cartId(item.getCart().getId())
                .productId(item.getProductId())
                .colorId(item.getColor() != null ? item.getColor().getId() : null)
                .storageId(item.getStorage() != null ? item.getStorage().getId() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice() * item.getQuantity())
                .addedAt(item.getAddedAt())
                .updatedAt(item.getUpdatedAt());

        // ✅ Ajouter le ProductDto complet
        if (product != null) {
            ProductDto productDto = ProductDto.builder()
                    .id(String.valueOf(product.getId()))
                    .name(product.getName())
                    .tagline(product.getTagline())
                    .price(product.getPrice())
                    .image(product.getImage())
                    .isFeatured(product.isFeatured())
                    .isNew(product.isNew())
                    .isBestseller(product.isBestseller())
                    .build();

            builder.product(productDto);
            log.debug("📦 Produit enrichi: {}", product.getName());
        } else {
            log.warn("⚠️ Produit non trouvé: {}", item.getProductId());
        }

        // ✅ Ajouter color et storage si disponibles
        if (item.getColor() != null) {
            ProductColorDto colorDto = ProductColorDto.builder()
                    .id(item.getColor().getId())
                    .name(item.getColor().getName())
                    .hex(item.getColor().getHex())
                    .code(item.getColor().getCode())
                    .image(item.getColor().getImage())
                    .priceAdjustment(item.getColor().getPriceAdjustment())
                    .available(item.getColor().getAvailable())
                    .build();
            builder.color(colorDto);
        }

        if (item.getStorage() != null) {
            ProductStorageDto storageDto = ProductStorageDto.builder()
                    .id(item.getStorage().getId())
                    .size(item.getStorage().getSize())
                    .price(item.getStorage().getPrice())
                    .available(item.getStorage().getAvailable())
                    .build();
            builder.storage(storageDto);
        }

        return builder.build();
    }

    @Data
    public static class CartCountResponse {
        private Integer count;

        public CartCountResponse(Integer count) {
            this.count = count;
        }
    }
}


