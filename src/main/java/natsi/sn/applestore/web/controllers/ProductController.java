package natsi.sn.applestore.web.controllers;

import natsi.sn.applestore.services.impl.ProductServiceImpl;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.CategoryDto;
import natsi.sn.applestore.web.dto.ProductColorDto;
import natsi.sn.applestore.web.dto.ProductStorageDto;
import natsi.sn.applestore.web.dto.ProductFeaturedDto;
import natsi.sn.applestore.web.dto.ProductSpecsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
//@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductServiceImpl productService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable String id) {
        ProductDto product = productService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductDto>> getFeaturedProducts() {
        return ResponseEntity.ok(productService.getFeaturedProducts());
    }

    @GetMapping("/new")
    public ResponseEntity<List<ProductDto>> getNewProducts() {
        return ResponseEntity.ok(productService.getNewProducts());
    }

    @GetMapping("/bestsellers")
    public ResponseEntity<List<ProductDto>> getBestsellers() {
        return ResponseEntity.ok(productService.getBestsellers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @GetMapping("/{id}/colors")
    public ResponseEntity<List<ProductColorDto>> getProductColors(@PathVariable String id) {
        try {
            Integer productId = Integer.parseInt(id);
            return ResponseEntity.ok(productService.getProductColors(productId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/storage")
    public ResponseEntity<List<ProductStorageDto>> getProductStorage(@PathVariable String id) {
        try {
            Integer productId = Integer.parseInt(id);
            return ResponseEntity.ok(productService.getProductStorage(productId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/features")
    public ResponseEntity<List<ProductFeaturedDto>> getProductFeatures(@PathVariable String id) {
        try {
            Integer productId = Integer.parseInt(id);
            return ResponseEntity.ok(productService.getProductFeatures(productId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/specs")
    public ResponseEntity<List<ProductSpecsDto>> getProductSpecs(@PathVariable String id) {
        try {
            Integer productId = Integer.parseInt(id);
            return ResponseEntity.ok(productService.getProductSpecs(productId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/track-view")
    public ResponseEntity<Void> trackProductView(@PathVariable String id, @RequestBody(required = false) String sessionId) {
        // TODO: Implémenter le tracking
        return ResponseEntity.ok().build();
    }
}

