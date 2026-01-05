package com.supabase.auth.config;

import com.supabase.auth.security.SupabaseJwtAuthenticationFilter;
import com.supabase.auth.security.SupabaseUserDetailsService;
import com.supabase.auth.service.SupabaseAuthService;
import com.supabase.auth.service.UserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration principale du module d'authentification Supabase
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SupabaseAuthProperties.class)
@ConditionalOnProperty(prefix = "supabase.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SupabaseAuthModuleConfig {

    @Bean
    public WebClient supabaseWebClient(SupabaseAuthProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getUrl() + "/auth/v1")
                .defaultHeader("apikey", properties.getAnonKey())
                .build();
    }

    @Bean
    public SupabaseAuthService supabaseAuthService(
            SupabaseAuthProperties properties,
            WebClient supabaseWebClient) {
        return new SupabaseAuthService(properties, supabaseWebClient);
    }
}
