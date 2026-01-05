package natsi.sn.applestore.config;

import com.supabase.auth.config.SupabaseAuthProperties;
import com.supabase.auth.model.SupabaseUser;
import com.supabase.auth.security.SupabaseJwtAuthenticationFilter;
import com.supabase.auth.security.SupabaseUserDetails;
import com.supabase.auth.security.SupabaseUserDetailsService;
import com.supabase.auth.service.SupabaseAuthService;
import com.supabase.auth.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import natsi.sn.applestore.data.enums.Role;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Configuration d'intégration du module Supabase Auth avec le projet Apple Store
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SupabaseAuthProperties.class)
public class SupabaseAuthIntegrationConfig {

    private final SupabaseAuthProperties supabaseAuthProperties;
    private final UserRepository userRepository;

    /**
     * WebClient pour les appels HTTP à Supabase
     */
    @Bean
    public WebClient supabaseWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseAuthProperties.getUrl() + "/auth/v1")
                .defaultHeader("apikey", supabaseAuthProperties.getAnonKey())
                .build();
    }

    /**
     * Service Supabase Auth (créé explicitement pour garantir sa disponibilité)
     */
    @Bean
    public SupabaseAuthService supabaseAuthService(WebClient supabaseWebClient) {
        return new SupabaseAuthService(supabaseAuthProperties, supabaseWebClient);
    }

    /**
     * Service de synchronisation des utilisateurs Supabase avec le backend local
     */
    @Bean
    public UserSyncService<User> userSyncService() {
        return new UserSyncService<>(
                supabaseAuthProperties,
                userRepository,
                this::mapSupabaseUserToUser,
                User::getEmail,
                User::getOauthId,
                this::updateUserFromSupabase
        );
    }

    /**
     * Service pour charger les détails de l'utilisateur depuis le backend local
     */
    @Bean
    public SupabaseUserDetailsService<User> supabaseUserDetailsService(SupabaseAuthService supabaseAuthService) {
        return new SupabaseUserDetailsService<>(
                supabaseAuthService,
                userRepository,
                User::getEmail,
                User::getId,
                User::getNomcomplet,
                user -> List.of(user.getRole().name()),
                userRepository::findByEmail,
                oauthId -> userRepository.findByOauthProviderAndOauthId("GOOGLE", oauthId)
        );
    }

    /**
     * Filtre JWT Supabase pour valider les tokens dans les requêtes
     */
    @Bean
    public SupabaseJwtAuthenticationFilter supabaseJwtAuthenticationFilter(
            SupabaseAuthService supabaseAuthService,
            SupabaseUserDetailsService<User> userDetailsService) {
        return new SupabaseJwtAuthenticationFilter(supabaseAuthService, userDetailsService);
    }

    /**
     * Mappe un utilisateur Supabase vers un utilisateur local (création)
     */
    private User mapSupabaseUserToUser(SupabaseUser supabaseUser) {
        User user = new User();
        user.setEmail(supabaseUser.getEmail());
        user.setNomcomplet(supabaseUser.getFullName());
        user.setPhone(supabaseUser.getPhone());
        user.setOauthProvider(supabaseUser.getOAuthProvider());
        user.setOauthId(supabaseUser.getOAuthId());
        user.setRole(Role.CLIENT); // Rôle par défaut
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        // Pour les utilisateurs OAuth, on génère un mot de passe aléatoire (non utilisé)
        user.setPassword("OAUTH_USER_" + System.currentTimeMillis());
        return user;
    }

    /**
     * Met à jour un utilisateur local avec les données Supabase
     */
    private User updateUserFromSupabase(SupabaseUser supabaseUser) {
        Optional<User> existingUserOpt = userRepository.findByEmail(supabaseUser.getEmail());

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            user.setNomcomplet(supabaseUser.getFullName());
            user.setPhone(supabaseUser.getPhone());
            user.setOauthProvider(supabaseUser.getOAuthProvider());
            user.setOauthId(supabaseUser.getOAuthId());
            if (supabaseUser.getLastSignInAt() != null) {
                user.setLastLogin(supabaseUser.getLastSignInAt());
            }
            return user;
        }

        // Si l'utilisateur n'existe pas, le créer
        return mapSupabaseUserToUser(supabaseUser);
    }

    /**
     * Intègre le filtre JWT Supabase dans la chaîne de sécurité existante
     * Cette méthode modifie la SecurityConfig existante pour ajouter le filtre Supabase
     */
    // Note: Le filtre sera ajouté dans SecurityConfig via @Autowired
}
