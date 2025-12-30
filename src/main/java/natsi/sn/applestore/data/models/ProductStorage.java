package natsi.sn.applestore.data.models;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(name="product_storage")
public class ProductStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;
    private Double price;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
