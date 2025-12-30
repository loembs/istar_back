package natsi.sn.applestore.data.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @ManyToOne
    @JoinColumn(name = "color_id")
    private ProductColor color;

    @ManyToOne
    @JoinColumn(name = "storage_id")
    private ProductStorage storage;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "color_name")
    private String colorName;

    @Column(name = "storage_size")
    private String storageSize;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    private LocalDateTime createdAt = LocalDateTime.now();
}

