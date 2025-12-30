package natsi.sn.applestore.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        // Convertir et uploader en WebP
        Map<String, Object> params = ObjectUtils.asMap(
                "folder", "apple-store",
                "format", "webp",
                "quality", "auto:good",
                "fetch_format", "auto"
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    public String uploadImageFromUrl(String imageUrl) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
                "folder", "apple-store",
                "format", "webp",
                "quality", "auto:good",
                "fetch_format", "auto"
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(imageUrl, params);
        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}

