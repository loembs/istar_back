package natsi.sn.applestore.data.models;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(name="product")
public class Product {
    @Id
    private int id;

    private String name;
    private String tagline;

    private Double price;

    private String image;

    @Column(name="is_featured")
    private boolean isFeatured;

    @Column(name="is_new")
    private boolean isNew;

    @Column(name="is_bestseller")
    private boolean isBestseller;

    @ManyToOne
    @JoinColumn(name="category_id")
    private Category category;




    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductColor> colors = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductStorage> storageOptions = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductFeature> features = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpecs> specs = new ArrayList<>();


}
