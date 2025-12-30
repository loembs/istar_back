package natsi.sn.applestore.utils.mappers.impl;

import natsi.sn.applestore.data.models.Product;
import natsi.sn.applestore.utils.mappers.ProductMapper;
import natsi.sn.applestore.web.dto.ProductDto;
import natsi.sn.applestore.web.dto.response.ProductResponseDto;

import java.util.List;

public class ProductMapperImpl implements ProductMapper {
    @Override
    public ProductResponseDto toDto(Product product) {
        ProductResponseDto dto=new ProductResponseDto();

        dto.setId(String.valueOf(product.getId()));
        dto.setName(product.getName());
        dto.setImage(product.getImage());
        dto.setPrice(product.getPrice());
        dto.setColors(List.of((natsi.sn.applestore.web.dto.ProductColorDto) product.getColors()));

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(String.valueOf(product.getCategory()));
        }
        return dto;
    }

    @Override
    public Product toEntity(ProductDto productDto) {
        return null;
    }
}
