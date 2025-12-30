package natsi.sn.applestore.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStorageDto {
    private Long id;
    private String size;
    private Double price;
    private Boolean available;
}
