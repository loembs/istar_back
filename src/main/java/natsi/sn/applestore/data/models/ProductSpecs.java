package natsi.sn.applestore.data.models;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(name = "product_specs")
public class ProductSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spec_key")
    private String specName;

    @Column(name = "spec_value")
    private String specValue;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
