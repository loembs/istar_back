package com.supabase.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration automatique du module d'authentification Supabase
 * Cette classe est chargée automatiquement via spring.factories
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SupabaseAuthProperties.class)
@ConditionalOnProperty(prefix = "supabase.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SupabaseAuthAutoConfiguration {

    public SupabaseAuthAutoConfiguration() {
        log.info("Module d'authentification Supabase chargé");
    }
}
