package natsi.sn.applestore.web.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CorsController {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    // Le CorsController n'est plus nécessaire car la configuration CORS est gérée par WebConfig et SecurityConfig
    // On garde seulement le endpoint health pour vérifier que le backend fonctionne
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Backend is running!");
    }
}


