package natsi.sn.applestore.services;

import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.CategoryDto;
import natsi.sn.applestore.web.dto.ProductColorDto;
import natsi.sn.applestore.web.dto.ProductStorageDto;
import natsi.sn.applestore.web.dto.ProductFeaturedDto;
import natsi.sn.applestore.web.dto.ProductSpecsDto;

import java.util.List;

public interface ProductService {
    List<ProductDto> getAllProducts();
    List<ProductDto> getProductsByCategory(String category);
    ProductDto getProductById(String id);
    List<ProductDto> getFeaturedProducts();
    List<ProductDto> getNewProducts();
    List<ProductDto> getBestsellers();
    List<ProductDto> searchProducts(String query);
    List<CategoryDto> getAllCategories();
    List<ProductColorDto> getProductColors(Integer productId);
    List<ProductStorageDto> getProductStorage(Integer productId);
    List<ProductFeaturedDto> getProductFeatures(Integer productId);
    List<ProductSpecsDto> getProductSpecs(Integer productId);
}
