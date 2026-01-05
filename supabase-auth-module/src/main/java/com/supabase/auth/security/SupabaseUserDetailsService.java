package com.supabase.auth.security;

import com.supabase.auth.service.SupabaseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service pour charger les détails de l'utilisateur depuis le backend local
 * 
 * @param <T> Type de l'entité User dans le backend
 */
@Slf4j
@RequiredArgsConstructor
public class SupabaseUserDetailsService<T> {

    private final SupabaseAuthService supabaseAuthService;
    private final JpaRepository<T, Long> userRepository;
    private final Function<T, String> emailExtractor;
    private final Function<T, Long> idExtractor;
    private final Function<T, String> fullNameExtractor;
    private final Function<T, List<String>> rolesExtractor;
    private final Function<String, Optional<T>> findByEmailFunction;
    private final Function<String, Optional<T>> findByOAuthIdFunction;

    /**
     * Charge un utilisateur par son ID Supabase et email
     */
    public SupabaseUserDetails loadUserBySupabaseId(String supabaseId, String email) {
        try {
            // Chercher d'abord par email
            Optional<T> userOpt = findByEmailFunction.apply(email);
            
            if (userOpt.isEmpty()) {
                // Chercher par OAuth ID
                userOpt = findByOAuthIdFunction.apply(supabaseId);
            }

            if (userOpt.isPresent()) {
                T user = userOpt.get();
                Long localId = idExtractor.apply(user);
                String fullName = fullNameExtractor.apply(user);
                List<String> roles = rolesExtractor.apply(user);

                return new SupabaseUserDetails(
                    supabaseId,
                    email,
                    localId,
                    fullName,
                    roles
                );
            }

            log.warn("Utilisateur non trouvé pour Supabase ID: {} / Email: {}", supabaseId, email);
            return null;
        } catch (Exception e) {
            log.error("Erreur lors du chargement de l'utilisateur: {}", e.getMessage(), e);
            return null;
        }
    }
}
