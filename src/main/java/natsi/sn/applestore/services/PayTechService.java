package natsi.sn.applestore.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Service d'intégration PayTech
 * Documentation: https://doc.paytech.sn/
 */
@Slf4j
@Service
public class PayTechService {

    private final RestTemplate restTemplate;

    @Value("${paytech.api.url:https://paytech.sn/api}")
    private String apiUrl;

    @Value("${paytech.api.key:}")
    private String apiKey;

    @Value("${paytech.api.secret:}")
    private String apiSecret;

    @Value("${paytech.ipn.url:}")
    private String ipnUrl;

    @Value("${paytech.success.url:}")
    private String successUrl;

    @Value("${paytech.cancel.url:}")
    private String cancelUrl;

    @Value("${paytech.production.mode:false}")
    private boolean productionMode;

    public PayTechService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envoie une demande de paiement à PayTech
     * @param idTransaction ID de la transaction (généré par le marchand)
     * @param amount Montant en FCFA
     * @param paymentTitle Titre de la commande
     * @param customField Données additionnelles (généralement l'ID de la commande)
     * @return Réponse JSON de PayTech contenant le token
     */
    public String sendPaymentRequest(String idTransaction, int amount, String paymentTitle, String customField) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("API_KEY", apiKey);
            headers.set("API_SECRET", apiSecret);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("item_name", paymentTitle);
            requestBody.put("item_price", amount);
            requestBody.put("currency", "XOF");
            requestBody.put("ref_command", idTransaction);
            requestBody.put("command_name", paymentTitle);
            requestBody.put("env", productionMode ? "prod" : "test");
            requestBody.put("ipn_url", ipnUrl);
            requestBody.put("success_url", successUrl);
            requestBody.put("cancel_url", cancelUrl);
            if (customField != null && !customField.isEmpty()) {
                requestBody.put("custom_field", customField);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Envoi de la demande de paiement PayTech pour la transaction: {}", idTransaction);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/payment/request-payment",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Demande de paiement PayTech réussie pour la transaction: {}", idTransaction);
                return response.getBody();
            } else {
                log.error("Erreur lors de la demande de paiement PayTech. Status: {}", response.getStatusCode());
                throw new RuntimeException("Erreur lors de la demande de paiement PayTech");
            }
        } catch (Exception e) {
            log.error("Exception lors de l'envoi de la demande de paiement PayTech: ", e);
            throw new RuntimeException("Erreur lors de l'envoi de la demande de paiement: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie que la notification IPN provient bien de PayTech
     * En comparant le hash SHA256 de la clé API et de la clé secrète
     *
     * @param receivedApiKeySha256 Hash SHA256 de la clé API reçu de PayTech
     * @param receivedApiSecretSha256 Hash SHA256 de la clé secrète reçu de PayTech
     * @return true si la notification provient de PayTech, false sinon
     */
    public boolean ipnFromPayTech(String receivedApiKeySha256, String receivedApiSecretSha256) {
        try {
            String expectedApiKeySha256 = sha256(apiKey);
            String expectedApiSecretSha256 = sha256(apiSecret);

            boolean isValid = expectedApiKeySha256.equals(receivedApiKeySha256) &&
                    expectedApiSecretSha256.equals(receivedApiSecretSha256);

            if (!isValid) {
                log.warn("IPN PayTech non valide. Hash API_KEY attendu: {}, reçu: {}. Hash API_SECRET attendu: {}, reçu: {}",
                        expectedApiKeySha256, receivedApiKeySha256, expectedApiSecretSha256, receivedApiSecretSha256);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'IPN PayTech: ", e);
            return false;
        }
    }

    /**
     * Calcule le hash SHA256 d'une chaîne
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Erreur lors du calcul du hash SHA256: ", e);
            throw new RuntimeException("Erreur lors du calcul du hash SHA256", e);
        }
    }

    /**
     * Vérifie le statut d'un paiement
     */
    public Map<String, Object> verifyPayment(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("API_KEY", apiKey);
            headers.set("API_SECRET", apiSecret);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/payment/verify/" + token,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            return response.getBody() != null ? response.getBody() : new HashMap<>();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du paiement PayTech: ", e);
            throw new RuntimeException("Erreur lors de la vérification du paiement: " + e.getMessage(), e);
        }
    }
}
