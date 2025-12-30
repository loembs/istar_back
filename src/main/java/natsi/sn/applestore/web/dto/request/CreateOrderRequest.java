package natsi.sn.applestore.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotEmpty(message = "La commande doit contenir au moins un article")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "L'adresse de livraison est obligatoire")
    @Valid
    private ShippingAddressRequest shippingAddress;

    @NotNull(message = "La méthode de paiement est obligatoire")
    private String paymentMethod; // CARD, PAYPAL, APPLE_PAY

    private String notes;
}



