package natsi.sn.applestore.web.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String paymentMethod;
    private Double amount;
    private String status; // PENDING, PAID, FAILED, REFUNDED
    private String transactionId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



