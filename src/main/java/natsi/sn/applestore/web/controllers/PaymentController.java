package natsi.sn.applestore.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import natsi.sn.applestore.data.enums.OrderStatus;
import natsi.sn.applestore.data.enums.PaymentStatus;
import natsi.sn.applestore.data.models.Order;
import natsi.sn.applestore.data.repository.OrderRepository;
import natsi.sn.applestore.services.PayTechService;
import natsi.sn.applestore.web.dto.request.PayTechPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller pour les paiements PayTech
 * Documentation PayTech: https://doc.paytech.sn/
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;
    private final PayTechService payTechService;

    // ========== PayTech Integration Methods ==========

    /**
     * Endpoint appelé par le SDK JS PayTech pour demander un token de paiement
     * URL: POST /api/payments/requestPayment
     *
     * Ce endpoint est appelé par le SDK JavaScript PayTech après que l'utilisateur
     * ait cliqué sur le bouton de paiement
     */
    @PostMapping("/requestPayment")
    public ResponseEntity<String> requestPayment(
            @RequestBody PayTechPaymentRequest request,
            Authentication authentication) {
        try {
            String idTransaction = request.getIdTransaction();
            log.info("Demande de token PayTech pour la transaction: {}", idTransaction);

            // Récupérer la commande depuis l'ID de transaction (qui contient l'ID de commande)
            // L'idTransaction peut être au format "ORDER_{orderId}"
            Long orderId;
            try {
                if (idTransaction.startsWith("ORDER_")) {
                    orderId = Long.parseLong(idTransaction.substring(6));
                } else {
                    orderId = Long.parseLong(idTransaction);
                }
            } catch (NumberFormatException e) {
                log.error("Format d'ID de transaction invalide: {}", idTransaction);
                return ResponseEntity.badRequest()
                        .body("{\"error\":\"Format d'ID de transaction invalide\"}");
            }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

            // Préparer les données pour PayTech
            int amount = order.getTotalAmount().intValue();
            String paymentTitle = String.format("Paiement commande № %s de %d FCFA",
                    order.getOrderNumber(), amount);

            // Envoyer la demande de paiement à PayTech
            String payTechResponse = payTechService.sendPaymentRequest(
                    idTransaction,
                    amount,
                    paymentTitle,
                    String.valueOf(order.getId()) // custom_field: ID de la commande
            );

            log.info("Réponse PayTech reçue pour la transaction {}: {}", idTransaction, payTechResponse);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(payTechResponse);
        } catch (Exception e) {
            log.error("Erreur lors de la demande de paiement PayTech: ", e);
            String errorMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Erreur inconnue";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Erreur lors de la demande de paiement: " + errorMessage + "\"}");
        }
    }

    /**
     * Endpoint IPN (Instant Payment Notification) appelé par PayTech
     * URL: POST /api/payments/paymentIpn
     *
     * PayTech appelle cet endpoint pour notifier que le paiement a été validé
     */
    @PostMapping(value = "/paymentIpn", consumes = {"application/x-www-form-urlencoded", "application/json"})
    public ResponseEntity<String> handleIpn(@RequestParam Map<String, String> params) {
        try {
            log.info("Réception d'un IPN PayTech: {}", params);

            // Récupérer les paramètres de l'IPN
            String receivedApiKeySha256 = params.get("api_key_sha256");
            String receivedApiSecretSha256 = params.get("api_secret_sha256");
            String customField = params.get("custom_field"); // ID de la commande
            String itemPrice = params.get("item_price");
            String paymentMethod = params.get("payment_method");

            // Vérifier que l'IPN provient bien de PayTech
            if (!payTechService.ipnFromPayTech(receivedApiKeySha256, receivedApiSecretSha256)) {
                log.warn("IPN PayTech rejeté: la notification ne provient pas de PayTech");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("IPN KO NOT FROM PAYTECH");
            }

            // Récupérer la commande
            Long orderId;
            try {
                orderId = Long.parseLong(customField);
            } catch (NumberFormatException e) {
                log.error("Format d'ID de commande invalide dans custom_field: {}", customField);
                return ResponseEntity.badRequest()
                        .body("IPN KO INVALID ORDER ID");
            }

            Order order = orderRepository.findById(orderId)
                    .orElse(null);

            if (order == null) {
                log.error("Commande non trouvée pour l'ID: {}", orderId);
                return ResponseEntity.badRequest()
                        .body("IPN KO ORDER NOT FOUND");
            }

            // Mettre à jour le statut de la commande
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            log.info("Commande {} mise à jour avec succès après IPN PayTech. Méthode: {}, Montant: {}",
                    order.getOrderNumber(), paymentMethod, itemPrice);

            return ResponseEntity.ok("IPN OK");
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'IPN PayTech: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IPN KO ERROR: " + e.getMessage());
        }
    }

    /**
     * Page de redirection après paiement réussi
     * URL: GET /api/payments/paymentSuccess
     */
    @GetMapping("/paymentSuccess")
    public ResponseEntity<String> paymentSuccess(@RequestParam(required = false) String token) {
        log.info("Redirection vers la page de succès PayTech. Token: {}", token);

        // Rediriger vers la page frontend de confirmation
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/order-confirmation?status=success&token=" + (token != null ? token : ""))
                .build();
    }

    /**
     * Page de redirection après annulation du paiement
     * URL: GET /api/payments/paymentCanceled
     */
    @GetMapping("/paymentCanceled")
    public ResponseEntity<String> paymentCanceled(@RequestParam(required = false) String token) {
        log.info("Redirection vers la page d'annulation PayTech. Token: {}", token);

        // Rediriger vers la page de paiement avec un message d'annulation
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/payment?status=canceled&token=" + (token != null ? token : ""))
                .build();
    }
}


