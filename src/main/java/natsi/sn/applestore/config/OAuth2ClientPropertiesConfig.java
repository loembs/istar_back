package natsi.sn.applestore.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * Configuration pour OAuth2 Client Properties.
 * Permet d'utiliser "disabled" comme valeur par défaut sans erreur de validation.
 */
@Configuration
@EnableConfigurationProperties
public class OAuth2ClientPropertiesConfig {

    /**
     * Crée un bean OAuth2ClientProperties personnalisé qui permet "disabled" comme valeur valide.
     * Ce bean remplace le bean par défaut de Spring Boot pour éviter les erreurs de validation.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.security.oauth2.client")
    public OAuth2ClientProperties oAuth2ClientProperties() {
        return new CustomOAuth2ClientProperties();
    }

    /**
     * Classe personnalisée qui étend OAuth2ClientProperties et désactive la validation
     * lorsque le client-id est "disabled".
     */
    private static class CustomOAuth2ClientProperties extends OAuth2ClientProperties implements InitializingBean {
        @Override
        public void afterPropertiesSet() {
            // Filtrer les registrations avec client-id "disabled" avant la validation
            Map<String, OAuth2ClientProperties.Registration> registrations = getRegistration();
            if (registrations != null) {
                registrations.entrySet().removeIf(entry ->
                        "disabled".equals(entry.getValue().getClientId())
                );
            }
            // Appeler la validation parente seulement sur les registrations valides
            try {
                super.afterPropertiesSet();
            } catch (Exception e) {
                // Si toutes les registrations sont "disabled", ignorer l'erreur
                if (registrations == null || registrations.isEmpty()) {
                    return;
                }
                throw e;
            }
        }
    }
}

