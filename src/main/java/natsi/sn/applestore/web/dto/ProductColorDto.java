package natsi.sn.applestore.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorDto {
    private Long id;
    private String name;
    private String hex;
    private String code;
    private String image;
    private Double priceAdjustment;
    private Boolean available;
}
