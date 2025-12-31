package natsi.sn.applestore.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration conditionnelle pour OAuth2 Client.
 * Désactive la validation OAuth2 si GOOGLE_CLIENT_ID n'est pas défini.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Crée un bean OAuth2ClientProperties conditionnel.
     * Si GOOGLE_CLIENT_ID est vide, retourne un bean vide pour éviter les erreurs de validation.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "spring.security.oauth2.client.registration.google",
            name = "client-id",
            havingValue = "",
            matchIfMissing = true
    )
    public OAuth2ClientProperties emptyOAuth2ClientProperties() {
        return new OAuth2ClientProperties();
    }
}
