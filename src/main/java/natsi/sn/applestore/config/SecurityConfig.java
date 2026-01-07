package natsi.sn.applestore.config;

import com.supabase.auth.config.SupabaseAuthProperties;
import com.supabase.auth.security.SupabaseJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.repository.UserRepository;
import natsi.sn.applestore.security.JwtAuthentificationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(SupabaseAuthProperties.class)
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthentificationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final SupabaseAuthProperties supabaseAuthProperties;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Autowired(required = false) SupabaseJwtAuthenticationFilter supabaseJwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ✅ IMPORTANT: Politique de session STATELESS pour JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Permettre les requêtes OPTIONS (préflight CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Routes publiques
                        .requestMatchers(
                                "/api/auth/**",
                                "/health",
                                "/api/products/**",
                                "/api/categories/**"
                        ).permitAll()

                        // Webhooks Supabase
                        .requestMatchers(supabaseAuthProperties.getWebhookUrl() + "/**").permitAll()

                        // Routes admin (nécessite ROLE_ADMIN)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ✅ Routes protégées (nécessite authentification JWT)
                        .requestMatchers(
                                "/api/cart/**",
                                "/api/orders/**",
                                "/api/payments/**"
                        ).authenticated()

                        // Toutes les autres requêtes
                        .anyRequest().permitAll()
                )

                .authenticationProvider(authenticationProvider())

                // ✅ CRITIQUE: Décommenter cette ligne pour activer le filtre JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}