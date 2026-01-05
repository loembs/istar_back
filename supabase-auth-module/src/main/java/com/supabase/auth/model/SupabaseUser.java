package com.supabase.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modèle représentant un utilisateur Supabase
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupabaseUser {

    @JsonProperty("id")
    private String id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("email_confirmed_at")
    private LocalDateTime emailConfirmedAt;

    @JsonProperty("phone_confirmed_at")
    private LocalDateTime phoneConfirmedAt;

    @JsonProperty("last_sign_in_at")
    private LocalDateTime lastSignInAt;

    @JsonProperty("app_metadata")
    private Map<String, Object> appMetadata;

    @JsonProperty("user_metadata")
    private Map<String, Object> userMetadata;

    @JsonProperty("identities")
    private Identity[] identities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Identity {
        @JsonProperty("id")
        private String id;

        @JsonProperty("provider")
        private String provider;

        @JsonProperty("identity_data")
        private Map<String, Object> identityData;
    }

    /**
     * Récupère le nom complet depuis user_metadata
     */
    public String getFullName() {
        if (userMetadata != null) {
            Object fullName = userMetadata.get("full_name");
            if (fullName != null) {
                return fullName.toString();
            }
            Object name = userMetadata.get("name");
            if (name != null) {
                return name.toString();
            }
        }
        return email != null ? email.split("@")[0] : "";
    }

    /**
     * Récupère le provider OAuth (Google, etc.)
     */
    public String getOAuthProvider() {
        if (identities != null && identities.length > 0) {
            return identities[0].getProvider().toUpperCase();
        }
        return null;
    }

    /**
     * Récupère l'ID OAuth
     */
    public String getOAuthId() {
        if (identities != null && identities.length > 0) {
            return identities[0].getId();
        }
        return null;
    }
}
