package natsi.sn.applestore.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import natsi.sn.applestore.web.dto.ProductDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private List<OrderItemResponse> items;
    private Double totalAmount;
    private String status;
    private ShippingAddressResponse shippingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private String notes;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private String productId;
        private Long colorId;
        private Long storageId;
        private String productName;
        private String colorName;
        private String storageSize;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
        private LocalDateTime createdAt;
        private ProductDto product;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressResponse {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String postalCode;
        private String country;
    }
}


