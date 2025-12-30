package natsi.sn.applestore.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private String id;
    private String name;
    private String tagline;
    private Double price;
    private String image;

    private boolean isFeatured;
    private boolean isNew;
    private boolean isBestseller;

    private CategoryDto category;

    private List<ProductColorDto> colors;
    private List<ProductStorageDto> storageOptions;
    private List<ProductFeaturedDto> features;
    private List<ProductSpecsDto> specs;
}
