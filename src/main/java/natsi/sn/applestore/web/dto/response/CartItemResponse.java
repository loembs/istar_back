package natsi.sn.applestore.web.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.ProductColorDto;
import natsi.sn.applestore.web.dto.ProductStorageDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long cartId;
    private String productId;
    private Long colorId;
    private Long storageId;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    // Relations
    private ProductDto product;
    private ProductColorDto color;
    private ProductStorageDto storage;
}



