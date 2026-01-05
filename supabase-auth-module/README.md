# Module d'Authentification Supabase pour Spring Boot

Module réutilisable pour intégrer l'authentification Supabase (avec Google OAuth) dans vos projets Spring Boot.

## Fonctionnalités

- ✅ Authentification avec Supabase (Google OAuth)
- ✅ Validation automatique des tokens JWT Supabase
- ✅ Synchronisation automatique des utilisateurs Supabase → Backend local
- ✅ Webhook pour recevoir les événements Supabase en temps réel
- ✅ Configuration Spring Boot auto-configurable
- ✅ Compatible avec Spring Security existant

## Installation

### 1. Ajouter le module comme dépendance

Dans votre `pom.xml` :

```xml
<dependency>
    <groupId>com.supabase</groupId>
    <artifactId>supabase-auth-module</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- WebFlux pour les appels HTTP à Supabase -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 2. Configuration

Ajoutez dans votre `application.properties` :

```properties
# Configuration Supabase Auth
supabase.auth.enabled=true
supabase.auth.url=https://your-project.supabase.co
supabase.auth.anon-key=your-anon-key
supabase.auth.service-role-key=your-service-role-key
supabase.auth.jwt-secret=your-jwt-secret-base64
supabase.auth.auto-sync-enabled=true
supabase.auth.webhook-url=/api/webhooks/supabase
supabase.auth.webhook-secret=your-webhook-secret
supabase.auth.google-oauth-enabled=true
```

Ou via variables d'environnement :

```bash
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
SUPABASE_JWT_SECRET=your-jwt-secret-base64
SUPABASE_AUTO_SYNC_ENABLED=true
SUPABASE_WEBHOOK_SECRET=your-webhook-secret
```

### 3. Intégration dans votre projet

Créez une classe de configuration pour intégrer le module avec votre entité User :

```java
@Configuration
@RequiredArgsConstructor
public class SupabaseAuthIntegrationConfig {

    private final SupabaseAuthProperties supabaseAuthProperties;
    private final SupabaseAuthService supabaseAuthService;
    private final UserRepository userRepository;

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

    @Bean
    public SupabaseUserDetailsService<User> supabaseUserDetailsService() {
        return new SupabaseUserDetailsService<>(
                supabaseAuthService,
                userRepository,
                User::getEmail,
                User::getId,
                User::getFullName,
                user -> List.of(user.getRole().name()),
                userRepository::findByEmail,
                oauthId -> userRepository.findByOauthProviderAndOauthId("GOOGLE", oauthId)
        );
    }

    @Bean
    public SupabaseJwtAuthenticationFilter supabaseJwtAuthenticationFilter() {
        SupabaseUserDetailsService<User> userDetailsService = supabaseUserDetailsService();
        return new SupabaseJwtAuthenticationFilter(supabaseAuthService, userDetailsService);
    }

    private User mapSupabaseUserToUser(SupabaseUser supabaseUser) {
        User user = new User();
        user.setEmail(supabaseUser.getEmail());
        user.setFullName(supabaseUser.getFullName());
        user.setOauthProvider(supabaseUser.getOAuthProvider());
        user.setOauthId(supabaseUser.getOAuthId());
        // ... autres champs
        return user;
    }

    private User updateUserFromSupabase(SupabaseUser supabaseUser) {
        // Logique de mise à jour
        return user;
    }
}
```

### 4. Intégrer le filtre dans SecurityConfig

Modifiez votre `SecurityConfig` pour ajouter le filtre Supabase :

```java
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Autowired(required = false) SupabaseJwtAuthenticationFilter supabaseJwtFilter) throws Exception {
    
    http
        // ... votre configuration existante
        .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

## Utilisation

### Frontend - Connexion avec Google via Supabase

Dans votre frontend, utilisez le SDK Supabase :

```javascript
import { createClient } from '@supabase/supabase-js'

const supabase = createClient(
  'https://your-project.supabase.co',
  'your-anon-key'
)

// Connexion avec Google
const { data, error } = await supabase.auth.signInWithOAuth({
  provider: 'google',
  options: {
    redirectTo: 'http://localhost:3000/auth/callback'
  }
})
```

### Backend - Utiliser l'utilisateur authentifié

Dans vos contrôleurs, l'utilisateur authentifié est disponible via `Authentication` :

```java
@GetMapping("/api/orders")
public ResponseEntity<?> getOrders(Authentication authentication) {
    SupabaseUserDetails userDetails = (SupabaseUserDetails) authentication.getPrincipal();
    Long userId = userDetails.getLocalUserId();
    // Utiliser userId pour récupérer les commandes
    return ResponseEntity.ok(orders);
}
```

## Webhook Supabase

Le module expose un endpoint webhook pour recevoir les événements Supabase :

**Endpoint** : `POST /api/webhooks/supabase`

**Événements supportés** :
- `auth.users.insert` - Nouvel utilisateur créé
- `auth.users.update` - Utilisateur mis à jour
- `auth.users.delete` - Utilisateur supprimé

### Configuration du webhook dans Supabase

1. Allez dans votre dashboard Supabase
2. Database → Webhooks
3. Créez un nouveau webhook :
   - Table: `auth.users`
   - Events: `INSERT`, `UPDATE`, `DELETE`
   - URL: `https://your-backend.com/api/webhooks/supabase`
   - HTTP Method: `POST`
   - Secret: Configurez `SUPABASE_WEBHOOK_SECRET`

## Structure du Module

```
supabase-auth-module/
├── src/main/java/com/supabase/auth/
│   ├── config/
│   │   ├── SupabaseAuthProperties.java      # Propriétés de configuration
│   │   └── SupabaseAuthModuleConfig.java    # Configuration du module
│   ├── model/
│   │   └── SupabaseUser.java                 # Modèle utilisateur Supabase
│   ├── security/
│   │   ├── SupabaseJwtAuthenticationFilter.java  # Filtre JWT
│   │   ├── SupabaseUserDetails.java          # Détails utilisateur Spring Security
│   │   └── SupabaseUserDetailsService.java   # Service de chargement utilisateur
│   ├── service/
│   │   ├── SupabaseAuthService.java          # Service Supabase
│   │   └── UserSyncService.java              # Synchronisation utilisateurs
│   └── webhook/
│       └── SupabaseWebhookController.java    # Contrôleur webhook
└── pom.xml
```

## Notes importantes

1. **JWT Secret** : Le `jwt-secret` doit être la clé secrète JWT de Supabase (en base64). Vous la trouvez dans Supabase Dashboard → Settings → API → JWT Secret.

2. **Synchronisation** : La synchronisation automatique crée/met à jour les utilisateurs dans votre base de données locale lorsqu'ils se connectent via Supabase.

3. **Ordre des filtres** : Le filtre Supabase doit être ajouté avant le filtre JWT local pour avoir la priorité.

4. **Sécurité** : Assurez-vous de configurer le `webhook-secret` pour valider les requêtes webhook.

## Support

Pour toute question ou problème, consultez la documentation Supabase : https://supabase.com/docs
