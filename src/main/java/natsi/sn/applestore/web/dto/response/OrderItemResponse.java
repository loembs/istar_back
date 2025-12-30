package natsi.sn.applestore.web.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import natsi.sn.applestore.web.dto.ProductDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long orderId;
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

    // Relations
    private ProductDto product;
}



