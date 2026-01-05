package com.supabase.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour le module d'authentification Supabase
 */
@Data
@ConfigurationProperties(prefix = "supabase.auth")
public class SupabaseAuthProperties {

    /**
     * URL de l'API Supabase
     */
    private String url;

    /**
     * Clé publique (anon key) de Supabase
     */
    private String anonKey;

    /**
     * Clé secrète (service role key) de Supabase pour les opérations admin
     */
    private String serviceRoleKey;

    /**
     * JWT Secret pour valider les tokens Supabase
     */
    private String jwtSecret;

    /**
     * Activer ou désactiver le module
     */
    private boolean enabled = true;

    /**
     * Activer la synchronisation automatique des utilisateurs
     */
    private boolean autoSyncEnabled = true;

    /**
     * URL du webhook Supabase pour recevoir les événements
     */
    private String webhookUrl = "/api/webhooks/supabase";

    /**
     * Secret du webhook pour valider les requêtes
     */
    private String webhookSecret;

    /**
     * Activer Google OAuth
     */
    private boolean googleOAuthEnabled = true;
}
