package natsi.sn.applestore.web.dto.response;

import natsi.sn.applestore.web.dto.*;

import java.util.List;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private String id;
    private String name;
    private String tagline;
    private Double price;
    private String image;

    private boolean isFeatured;
    private boolean isNew;
    private boolean isBestseller;

    private int categoryId;
    private String categoryName;

    private List<ProductColorDto> colors;
    private List<ProductStorageDto> storageOptions;
    private List<ProductFeaturedDto> features;
    private List<ProductSpecsDto> specs;
}
