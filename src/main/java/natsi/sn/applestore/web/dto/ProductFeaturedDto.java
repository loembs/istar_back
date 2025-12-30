package natsi.sn.applestore.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFeaturedDto {
    private Long id;
    private String feature;
    private String name; // Alias pour compatibilité
}
