package com.supabase.auth.service;

import com.supabase.auth.config.SupabaseAuthProperties;
import com.supabase.auth.model.SupabaseUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service pour interagir avec l'API Supabase Auth
 */
@Slf4j
@Service
public class SupabaseAuthService {

    private final SupabaseAuthProperties properties;
    private final WebClient webClient;

    public SupabaseAuthService(SupabaseAuthProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    /**
     * Valide un token JWT Supabase
     */
    public Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    Base64.getUrlDecoder().decode(properties.getJwtSecret())
            );

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Erreur lors de la validation du token Supabase: {}", e.getMessage());
            throw new RuntimeException("Token invalide", e);
        }
    }

    /**
     * Récupère un utilisateur depuis Supabase par son ID
     */
    public Mono<SupabaseUser> getUserById(String userId) {
        return webClient.get()
                .uri("/admin/users/{id}", userId)
                .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                .retrieve()
                .bodyToMono(SupabaseUser.class)
                .doOnError(error -> log.error("Erreur lors de la récupération de l'utilisateur: {}", error.getMessage()));
    }

    /**
     * Récupère un utilisateur depuis Supabase par son email
     */
    public Mono<SupabaseUser> getUserByEmail(String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/users")
                        .queryParam("email", email)
                        .build())
                .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                .retrieve()
                .bodyToMono(SupabaseUser[].class)
                .map(users -> users.length > 0 ? users[0] : null)
                .doOnError(error -> log.error("Erreur lors de la récupération de l'utilisateur par email: {}", error.getMessage()));
    }

    /**
     * Récupère l'utilisateur actuel depuis le token
     */
    public Mono<SupabaseUser> getCurrentUser(String token) {
        Claims claims = validateToken(token);
        String userId = claims.getSubject();
        return getUserById(userId);
    }

    /**
     * Extrait l'email depuis le token
     */
    public String extractEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extrait l'ID utilisateur depuis le token
     */
    public String extractUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }
}
