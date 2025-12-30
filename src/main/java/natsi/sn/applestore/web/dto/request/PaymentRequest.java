package natsi.sn.applestore.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull(message = "L'ID de la commande est obligatoire")
    private Long orderId;

    @NotBlank(message = "La méthode de paiement est obligatoire")
    private String paymentMethod; // CARD, PAYPAL, APPLE_PAY

    @NotNull(message = "Le montant est obligatoire")
    @Min(value = 0, message = "Le montant doit être positif")
    private Double amount;
}

