package natsi.sn.applestore.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.Order;
import natsi.sn.applestore.data.enums.*;
import natsi.sn.applestore.data.models.OrderItem;
import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.OrderItemRepository;
import natsi.sn.applestore.data.repository.OrderRepository;
import natsi.sn.applestore.data.repository.ProductRepository;
import natsi.sn.applestore.web.dto.request.CreateOrderRequest;
import natsi.sn.applestore.web.dto.request.OrderItemRequest;
import natsi.sn.applestore.web.dto.response.ApiResponse;
import natsi.sn.applestore.web.dto.response.OrderResponse;
import natsi.sn.applestore.web.dto.response.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();

            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("La commande doit contenir au moins un article"));
            }

            if (request.getShippingAddress() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("L'adresse de livraison est obligatoire"));
            }

            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(0.0);

            Order.ShippingAddress shippingAddr = convertShippingAddressRequest(request.getShippingAddress());
            if (shippingAddr.getPhone() == null || shippingAddr.getPhone().trim().isEmpty()) {
                shippingAddr.setPhone("N/A");
            }

            order.setShippingAddress(shippingAddr);
            order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setNotes(request.getNotes());

            order = orderRepository.save(order);

            List<OrderItem> orderItems = createOrderItems(order, request.getItems());
            orderItems = orderItemRepository.saveAll(orderItems);

            order.setOrderItems(orderItems);

            Double totalAmount = calculateOrderTotal(orderItems);
            order.setTotalAmount(totalAmount);
            order = orderRepository.save(order);

            return ResponseEntity.ok(ApiResponse.success(convertToDto(order), "Commande créée avec succès"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la création de la commande: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur interne du serveur: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orders = orderRepository.findByUser(user, pageable);
        PaginatedResponse<OrderResponse> response = PaginatedResponse.of(orders.map(this::convertToDto));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return orderRepository.findById(id)
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .map(this::convertToDto)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.ok(ApiResponse.error("Commande non trouvée")));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return orderRepository.findByOrderNumber(orderNumber)
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .map(this::convertToDto)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.ok(ApiResponse.error("Commande non trouvée")));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Order> orders = orderRepository.findByUser(user);

        List<OrderResponse> orderDtos = orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(orderDtos));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return orderRepository.findById(id)
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .filter(order -> order.getStatus() == OrderStatus.PENDING ||
                        order.getStatus() == OrderStatus.CONFIRMED)
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    if (reason != null) {
                        order.setNotes((order.getNotes() != null ? order.getNotes() : "") + "\nAnnulation: " + reason);
                    }
                    order = orderRepository.save(order);
                    return ResponseEntity.ok(ApiResponse.success(convertToDto(order), "Commande annulée"));
                })
                .orElse(ResponseEntity.ok(ApiResponse.error("Commande non trouvée ou ne peut pas être annulée")));
    }

    private String generateOrderNumber() {
        return "APP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(item -> {
                    Product product = productRepository.findById(Integer.parseInt(item.getProductId()))
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + item.getProductId()));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(item.getProductId());
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setUnitPrice(product.getPrice());
                    orderItem.setTotalPrice(product.getPrice() * item.getQuantity());
                    orderItem.setProductName(product.getName());

                    // TODO: Récupérer color et storage si nécessaire

                    return orderItem;
                })
                .collect(Collectors.toList());
    }

    private Double calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
    }

    private OrderResponse convertToDto(Order order) {
        List<OrderResponse.OrderItemResponse> itemDtos = new ArrayList<>();

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            itemDtos = order.getOrderItems().stream()
                    .map(this::convertOrderItemToDto)
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .items(itemDtos)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(convertShippingAddressToDto(order.getShippingAddress()))
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .notes(order.getNotes())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponse.OrderItemResponse convertOrderItemToDto(OrderItem orderItem) {
        return OrderResponse.OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .colorId(orderItem.getColor() != null ? orderItem.getColor().getId() : null)
                .storageId(orderItem.getStorage() != null ? orderItem.getStorage().getId() : null)
                .productName(orderItem.getProductName())
                .colorName(orderItem.getColorName())
                .storageSize(orderItem.getStorageSize())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }

    private Order.ShippingAddress convertShippingAddressRequest(natsi.sn.applestore.web.dto.request.ShippingAddressRequest request) {
        if (request == null) {
            return null;
        }
        Order.ShippingAddress address = new Order.ShippingAddress();
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setEmail(request.getEmail());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        return address;
    }

    private OrderResponse.ShippingAddressResponse convertShippingAddressToDto(Order.ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return OrderResponse.ShippingAddressResponse.builder()
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .email(address.getEmail())
                .phone(address.getPhone())
                .address(address.getAddress())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }
}


