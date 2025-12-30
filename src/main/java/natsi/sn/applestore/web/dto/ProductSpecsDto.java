package natsi.sn.applestore.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecsDto {
    private Long id;
    private String specName;
    private String specValue;
    private String name; // Alias pour compatibilité
    private String value; // Alias pour compatibilité
}
