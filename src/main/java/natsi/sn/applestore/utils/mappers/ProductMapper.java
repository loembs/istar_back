package natsi.sn.applestore.utils.mappers;

import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.response.ProductResponseDto;

public interface ProductMapper {
    ProductResponseDto toDto(Product product);
    Product toEntity(ProductDto productDto);
}
