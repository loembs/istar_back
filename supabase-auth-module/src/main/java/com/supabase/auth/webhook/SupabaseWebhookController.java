package com.supabase.auth.webhook;

import com.supabase.auth.config.SupabaseAuthProperties;
import com.supabase.auth.model.SupabaseUser;
import com.supabase.auth.service.SupabaseAuthService;
import com.supabase.auth.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Contrôleur pour recevoir les webhooks Supabase
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/supabase")
@RequiredArgsConstructor
public class SupabaseWebhookController {

    private final SupabaseAuthProperties properties;
    private final SupabaseAuthService supabaseAuthService;
    private final UserSyncService<?> userSyncService;

    /**
     * Endpoint pour recevoir les événements Supabase
     * Types d'événements: auth.users.insert, auth.users.update, auth.users.delete
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "x-supabase-signature", required = false) String signature,
            @RequestBody SupabaseWebhookPayload payload) {

        // Valider la signature si le secret est configuré
        if (properties.getWebhookSecret() != null && !properties.getWebhookSecret().isEmpty()) {
            if (!validateSignature(signature, payload.toString())) {
                log.warn("Signature webhook invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Signature invalide");
            }
        }

        try {
            String eventType = payload.getType();
            SupabaseUser user = payload.getRecord();

            log.info("Réception d'un webhook Supabase: {} pour l'utilisateur {}", eventType, user.getEmail());

            switch (eventType) {
                case "INSERT":
                case "UPDATE":
                    // Synchroniser l'utilisateur avec le backend local
                    userSyncService.syncUser(user);
                    break;
                case "DELETE":
                    // Gérer la suppression si nécessaire
                    log.info("Utilisateur supprimé: {}", user.getEmail());
                    break;
                default:
                    log.warn("Type d'événement non géré: {}", eventType);
            }

            return ResponseEntity.ok("Webhook traité avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du traitement du webhook");
        }
    }

    /**
     * Valide la signature du webhook
     */
    private boolean validateSignature(String signature, String payload) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return signature.equals(expectedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Erreur lors de la validation de la signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Classe pour désérialiser le payload du webhook Supabase
     */
    @lombok.Data
    public static class SupabaseWebhookPayload {
        private String type;
        private SupabaseUser record;
        private String table;
        private String schema;
    }
}
