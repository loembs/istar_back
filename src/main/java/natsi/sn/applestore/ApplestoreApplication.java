package natsi.sn.applestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApplestoreApplication {

    public static void main(String[] args) {
        // Désactiver Docker Compose via variable d'environnement
        System.setProperty("spring.docker.compose.enabled", "false");
        SpringApplication.run(ApplestoreApplication.class, args);
    }

}
