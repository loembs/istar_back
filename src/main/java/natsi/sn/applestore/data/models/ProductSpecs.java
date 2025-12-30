package natsi.sn.applestore.data.models;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
public class ProductSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String specName;
    private String specValue;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
