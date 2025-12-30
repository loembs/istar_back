package natsi.sn.applestore.data.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(name="category")
public class Category   {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;
    private String libelle;
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Product> products= new ArrayList<>();

}
