package natsi.sn.applestore.data.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(name="product_color")
public class ProductColor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String hex;
    private String code;
    private String image;

    @Column(name = "price_adjustment")
    private Double priceAdjustment = 0.0;

    private Boolean available = true;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
