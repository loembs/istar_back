package natsi.sn.applestore.web.controllers;

import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.enums.*;
import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.data.models.Order;
import natsi.sn.applestore.data.repository.OrderRepository;
import natsi.sn.applestore.data.repository.ProductRepository;
import natsi.sn.applestore.data.repository.CategoryRepository;
import natsi.sn.applestore.web.dto.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(Authentication authentication) {
        Map<String, Object> stats = new HashMap<>();

        // Statistiques des commandes
        stats.put("totalOrders", orderRepository.countTotalOrders());
        stats.put("totalRevenue", orderRepository.sumTotalRevenue());
        stats.put("pendingOrders", orderRepository.countOrdersByStatus(OrderStatus.PENDING));
        stats.put("completedOrders", orderRepository.countOrdersByStatus(OrderStatus.DELIVERED));

        // Statistiques des produits
        stats.put("totalProducts", productRepository.count());
        stats.put("featuredProducts", productRepository.findByIsFeaturedTrue().size());

        // Statistiques par catégorie
        List<Object[]> categoryStats = categoryRepository.findAll().stream()
                .map(category -> new Object[]{
                        category,
                        productRepository.findByCategoryId(category.getId()).size()
                })
                .collect(Collectors.toList());

        Map<String, Long> categoryCounts = new HashMap<>();
        for (Object[] stat : categoryStats) {
            natsi.sn.applestore.data.models.Category category = (natsi.sn.applestore.data.models.Category) stat[0];
            Integer countInt = (Integer) stat[1];
            Long count = countInt != null ? countInt.longValue() : 0L;
            categoryCounts.put(category.getLibelle(), count);
        }
        stats.put("categoryStats", categoryCounts);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Object>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findWithFilters(status, paymentStatus, null, null, pageable);

        Page<Object> orderSummaries = orders.map(order -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", order.getId());
            summary.put("orderNumber", order.getOrderNumber());
            summary.put("userId", order.getUser().getId());
            summary.put("totalAmount", order.getTotalAmount());
            summary.put("status", order.getStatus());
            summary.put("paymentStatus", order.getPaymentStatus());
            summary.put("createdAt", order.getCreatedAt());
            summary.put("customerEmail", order.getUser().getEmail());
            return summary;
        });

        return ResponseEntity.ok(ApiResponse.success(orderSummaries));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Object>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            // TODO: Implémenter la recherche par nom
            products = productRepository.findAll(pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        Page<Object> productSummaries = products.map(product -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", product.getId());
            summary.put("name", product.getName());
            summary.put("price", product.getPrice());
            summary.put("category", product.getCategory() != null ? product.getCategory().getLibelle() : null);
            summary.put("isFeatured", product.isFeatured());
            summary.put("isNew", product.isNew());
            summary.put("isBestseller", product.isBestseller());
            return summary;
        });

        return ResponseEntity.ok(ApiResponse.success(productSummaries));
    }

    @GetMapping("/recent-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(0, limit);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Order> orders = orderRepository.findRecentOrders(thirtyDaysAgo, pageable);

        List<Object> orderSummaries = orders.stream().map(order -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", order.getId());
            summary.put("orderNumber", order.getOrderNumber());
            summary.put("totalAmount", order.getTotalAmount());
            summary.put("status", order.getStatus());
            summary.put("createdAt", order.getCreatedAt());
            return summary;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(orderSummaries));
    }

    @GetMapping("/popular-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findByIsFeaturedTrue();

        List<Object> productSummaries = products.stream().limit(limit).map(product -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", product.getId());
            summary.put("name", product.getName());
            summary.put("price", product.getPrice());
            summary.put("category", product.getCategory() != null ? product.getCategory().getLibelle() : null);
            summary.put("isFeatured", product.isFeatured());
            return summary;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(productSummaries));
    }

    @GetMapping("/dashboard-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData(Authentication authentication) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Statistiques générales
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.countTotalOrders());
        stats.put("totalRevenue", orderRepository.sumTotalRevenue());
        stats.put("pendingOrders", orderRepository.countOrdersByStatus(OrderStatus.PENDING));
        stats.put("completedOrders", orderRepository.countOrdersByStatus(OrderStatus.DELIVERED));
        dashboardData.put("statistics", stats);

        // Commandes récentes
        List<Object> recentOrders = getRecentOrders(5, authentication).getBody().getData();
        dashboardData.put("recentOrders", recentOrders);

        // Produits populaires
        List<Object> popularProducts = getPopularProducts(5, authentication).getBody().getData();
        dashboardData.put("popularProducts", popularProducts);

        // Évolution des ventes (simulé)
        Map<String, Double> salesEvolution = new HashMap<>();
        salesEvolution.put("thisMonth", orderRepository.sumTotalRevenue());
        salesEvolution.put("lastMonth", orderRepository.sumTotalRevenue() * 0.85);
        dashboardData.put("salesEvolution", salesEvolution);

        return ResponseEntity.ok(ApiResponse.success(dashboardData));
    }
}

