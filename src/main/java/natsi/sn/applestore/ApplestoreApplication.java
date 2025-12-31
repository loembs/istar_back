package natsi.sn.applestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;

@SpringBootApplication
public class ApplestoreApplication {

    public static void main(String[] args) {
        // Désactiver Docker Compose via variable d'environnement
        System.setProperty("spring.docker.compose.enabled", "false");

        // Désactiver OAuth2 auto-configuration si GOOGLE_CLIENT_ID n'est pas défini ou est "disabled"
        String googleClientId = System.getenv("GOOGLE_CLIENT_ID");
        if (googleClientId == null || googleClientId.trim().isEmpty() || "disabled".equals(googleClientId)) {
            System.setProperty("spring.autoconfigure.exclude",
                    "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration");
        }

        SpringApplication.run(ApplestoreApplication.class, args);
    }

}
