package natsi.sn.applestore.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud_name:}")
    private String cloudName;

    @Value("${cloudinary.api_key:}")
    private String apiKey;

    @Value("${cloudinary.api_secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        // Si les variables ne sont pas définies, créer un bean avec des valeurs vides
        // L'application démarrera, mais l'upload échouera si Cloudinary n'est pas configuré
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName != null && !cloudName.isEmpty() ? cloudName : "");
        config.put("api_key", apiKey != null && !apiKey.isEmpty() ? apiKey : "");
        config.put("api_secret", apiSecret != null && !apiSecret.isEmpty() ? apiSecret : "");
        return new Cloudinary(config);
    }
}
