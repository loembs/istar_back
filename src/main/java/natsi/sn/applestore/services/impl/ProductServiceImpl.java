package natsi.sn.applestore.services.impl;

import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.data.repository.ProductRepository;
import natsi.sn.applestore.data.repository.CategoryRepository;
import natsi.sn.applestore.data.repository.ProductColorRepository;
import natsi.sn.applestore.data.repository.ProductStorageRepository;
import natsi.sn.applestore.data.repository.ProductFeatureRepository;
import natsi.sn.applestore.data.repository.ProductSpecsRepository;
import natsi.sn.applestore.services.ProductService;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.ProductColorDto;
import natsi.sn.applestore.web.dto.ProductStorageDto;
import natsi.sn.applestore.web.dto.ProductFeaturedDto;
import natsi.sn.applestore.web.dto.ProductSpecsDto;
import natsi.sn.applestore.web.dto.CategoryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductColorRepository productColorRepository;

    @Autowired
    private ProductStorageRepository productStorageRepository;

    @Autowired
    private ProductFeatureRepository productFeatureRepository;

    @Autowired
    private ProductSpecsRepository productSpecsRepository;



    @Override
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getProductsByCategory(String category) {
        // Chercher par ID de catégorie
        try {
            Integer categoryId = Integer.parseInt(category);
            return productRepository.findByCategoryId(categoryId).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // Si ce n'est pas un nombre, chercher par nom
            return productRepository.findAll().stream()
                    .filter(p -> p.getCategory() != null &&
                            (p.getCategory().getLibelle().equalsIgnoreCase(category) ||
                                    p.getCategory().getLibelle().toLowerCase().contains(category.toLowerCase())))
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public ProductDto getProductById(String id) {
        try {
            Integer productId = Integer.parseInt(id);
            Optional<Product> product = productRepository.findById(productId);
            return product.map(this::mapToDto).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<ProductDto> getFeaturedProducts() {
        return productRepository.findAll().stream()
                .filter(Product::isFeatured)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getNewProducts() {
        return productRepository.findAll().stream()
                .filter(Product::isNew)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getBestsellers() {
        return productRepository.findAll().stream()
                .filter(Product::isBestseller)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> searchProducts(String query) {
        String lowerQuery = query.toLowerCase();
        return productRepository.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery) ||
                        (p.getTagline() != null && p.getTagline().toLowerCase().contains(lowerQuery)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CategoryDto.builder()
                        .id((long) c.getId())
                        .libelle(c.getLibelle())
                        .name(c.getLibelle())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductColorDto> getProductColors(Integer productId) {
        return productColorRepository.findByProductId(productId).stream()
                .map(c -> ProductColorDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .hex(c.getHex())
                        .code(c.getCode())
                        .image(c.getImage())
                        .available(true)
                        .priceAdjustment(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductStorageDto> getProductStorage(Integer productId) {
        return productStorageRepository.findByProductId(productId).stream()
                .map(s -> ProductStorageDto.builder()
                        .id(s.getId())
                        .size(s.getSize())
                        .price(s.getPrice())
                        .available(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductFeaturedDto> getProductFeatures(Integer productId) {
        return productFeatureRepository.findByProductId(productId).stream()
                .map(f -> ProductFeaturedDto.builder()
                        .id(f.getId())
                        .feature(f.getFeature())
                        .name(f.getFeature())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSpecsDto> getProductSpecs(Integer productId) {
        return productSpecsRepository.findByProductId(productId).stream()
                .map(s -> ProductSpecsDto.builder()
                        .id(s.getId())
                        .specName(s.getSpecName())
                        .specValue(s.getSpecValue())
                        .name(s.getSpecName())
                        .value(s.getSpecValue())
                        .build())
                .collect(Collectors.toList());
    }

    private ProductDto mapToDto(Product product) {
        CategoryDto categoryDto = product.getCategory() != null
                ? CategoryDto.builder()
                .id(Long.valueOf(product.getCategory().getId()))
                .libelle(product.getCategory().getLibelle())
                .name(product.getCategory().getLibelle())
                .build()
                : null;

        List<ProductColorDto> colors = product.getColors() != null
                ? product.getColors().stream()
                .map(c -> ProductColorDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .hex(c.getHex())
                        .code(c.getCode())
                        .image(c.getImage())
                        .available(true)
                        .priceAdjustment(0.0)
                        .build())
                .collect(Collectors.toList())
                : List.of();

        List<ProductStorageDto> storage = product.getStorageOptions() != null
                ? product.getStorageOptions().stream()
                .map(s -> ProductStorageDto.builder()
                        .id(s.getId())
                        .size(s.getSize())
                        .price(s.getPrice())
                        .available(true)
                        .build())
                .collect(Collectors.toList())
                : List.of();

        List<ProductFeaturedDto> features = product.getFeatures() != null
                ? product.getFeatures().stream()
                .map(f -> ProductFeaturedDto.builder()
                        .id(f.getId())
                        .feature(f.getFeature())
                        .name(f.getFeature())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        List<ProductSpecsDto> specs = product.getSpecs() != null
                ? product.getSpecs().stream()
                .map(s -> ProductSpecsDto.builder()
                        .id(s.getId())
                        .specName(s.getSpecName())
                        .specValue(s.getSpecValue())
                        .name(s.getSpecName())
                        .value(s.getSpecValue())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return ProductDto.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .tagline(product.getTagline())
                .price(product.getPrice())
                .image(product.getImage())
                .isFeatured(product.isFeatured())
                .isNew(product.isNew())
                .isBestseller(product.isBestseller())
                .category(categoryDto)
                .colors(colors)
                .storageOptions(storage)
                .features(features)
                .specs(specs)
                .build();
    }
}
