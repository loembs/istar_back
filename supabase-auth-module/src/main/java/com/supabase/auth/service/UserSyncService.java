package com.supabase.auth.service;

import com.supabase.auth.config.SupabaseAuthProperties;
import com.supabase.auth.model.SupabaseUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.function.Function;

/**
 * Service générique pour synchroniser les utilisateurs Supabase avec le backend local
 * 
 * @param <T> Type de l'entité User dans le backend
 */
@Slf4j
@RequiredArgsConstructor
public class UserSyncService<T> {

    private final SupabaseAuthProperties properties;
    private final JpaRepository<T, Long> userRepository;
    private final Function<SupabaseUser, T> userMapper;
    private final Function<T, String> emailExtractor;
    private final Function<T, String> oauthIdExtractor;
    private final Function<SupabaseUser, T> userUpdater;

    /**
     * Synchronise un utilisateur Supabase avec le backend local
     * Crée l'utilisateur s'il n'existe pas, le met à jour sinon
     */
    public T syncUser(SupabaseUser supabaseUser) {
        if (!properties.isAutoSyncEnabled()) {
            log.debug("Synchronisation automatique désactivée");
            return null;
        }

        try {
            // Chercher par email d'abord
            Optional<T> existingUser = findUserByEmail(supabaseUser.getEmail());
            
            if (existingUser.isPresent()) {
                // Mettre à jour l'utilisateur existant
                T updatedUser = userUpdater.apply(supabaseUser);
                return userRepository.save(updatedUser);
            } else {
                // Créer un nouvel utilisateur
                T newUser = userMapper.apply(supabaseUser);
                return userRepository.save(newUser);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de l'utilisateur: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de synchronisation", e);
        }
    }

    /**
     * Trouve un utilisateur par email
     */
    private Optional<T> findUserByEmail(String email) {
        // Cette méthode doit être implémentée dans le repository
        // Pour l'instant, on utilise une approche générique
        return userRepository.findAll().stream()
                .filter(user -> email.equals(emailExtractor.apply(user)))
                .findFirst();
    }

    /**
     * Trouve un utilisateur par OAuth ID
     */
    public Optional<T> findUserByOAuthId(String oauthId) {
        return userRepository.findAll().stream()
                .filter(user -> oauthId.equals(oauthIdExtractor.apply(user)))
                .findFirst();
    }
}
