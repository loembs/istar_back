package natsi.sn.applestore.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

/**
 * Condition personnalisée pour vérifier que OAuth2 est activé
 * (client-id n'est pas vide ni "disabled").
 */
public class OAuth2EnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String clientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
        return clientId != null && !clientId.trim().isEmpty() && !"disabled".equals(clientId);
    }
}

