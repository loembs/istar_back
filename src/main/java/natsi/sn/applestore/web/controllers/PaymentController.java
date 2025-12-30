package natsi.sn.applestore.web.controllers;

import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.Order;
import natsi.sn.applestore.data.repository.OrderRepository;
import natsi.sn.applestore.services.PayTechService;
import natsi.sn.applestore.web.dto.request.PaymentRequest;
import natsi.sn.applestore.web.dto.response.ApiResponse;
import natsi.sn.applestore.web.dto.response.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;
    private final PayTechService payTechService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

            // Préparer les données pour PayTech
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("item_name", "Commande #" + order.getOrderNumber());
            paymentData.put("item_price", request.getAmount());
            paymentData.put("currency", "XOF");
            paymentData.put("ref_command", order.getOrderNumber());
            paymentData.put("command_name", "Apple Store Order");
            paymentData.put("env", "test"); // ou "prod" en production

            // Appeler PayTech
            Map<String, Object> paytechResponse = payTechService.initiatePayment(paymentData);

            String transactionId = (String) paytechResponse.get("token");
            String paymentUrl = (String) paytechResponse.get("redirect_url");

            PaymentResponse response = PaymentResponse.builder()
                    .id(1L)
                    .orderId(order.getId())
                    .paymentMethod(request.getPaymentMethod())
                    .amount(request.getAmount())
                    .status("PENDING")
                    .transactionId(transactionId)
                    .message("Paiement initié avec succès. Redirigez vers: " + paymentUrl)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "Paiement initié"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'initiation du paiement: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @RequestParam String transactionId) {
        try {
            Map<String, Object> verificationResult = payTechService.verifyPayment(transactionId);

            String status = (String) verificationResult.get("status");

            PaymentResponse response = PaymentResponse.builder()
                    .transactionId(transactionId)
                    .status(status)
                    .message("Paiement vérifié")
                    .updatedAt(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "Paiement vérifié"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la vérification: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook/flutterwave")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "verif-hash", required = false) String signature,
            @RequestBody Map<String, Object> webhookData) {

        // Vérification de la signature
        String secretHash = System.getenv("FLW_SECRET_HASH");
        if (secretHash != null && !secretHash.equals(signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        // Traitement du webhook
        Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
        if (data != null) {
            String status = (String) data.get("status");
            String txRef = (String) data.get("tx_ref");
            String transactionId = String.valueOf(data.get("id"));

            if ("successful".equalsIgnoreCase(status)) {
                // Mettre à jour le statut de commande
                // TODO: Implémenter la mise à jour du statut de paiement
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }
}


