package natsi.sn.applestore.web.controllers;

import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.services.ImageService;
import natsi.sn.applestore.web.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Le fichier est vide"));
            }

            String imageUrl = imageService.uploadImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("format", "webp");
            response.put("message", "Image uploadée et convertie en WebP avec succès");

            return ResponseEntity.ok(ApiResponse.success(response, "Image uploadée avec succès"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'upload: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-from-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImageFromUrl(
            @RequestParam("url") String imageUrl) {
        try {
            String uploadedUrl = imageService.uploadImageFromUrl(imageUrl);

            Map<String, String> response = new HashMap<>();
            response.put("url", uploadedUrl);
            response.put("format", "webp");
            response.put("message", "Image téléchargée et convertie en WebP avec succès");

            return ResponseEntity.ok(ApiResponse.success(response, "Image uploadée avec succès"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'upload: " + e.getMessage()));
        }
    }
}

