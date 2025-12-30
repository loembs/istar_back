package natsi.sn.applestore.data.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Integer totalItems = 0;
    private Double totalPrice = 0.0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    public void calculateTotals() {
        if (items != null && !items.isEmpty()) {
            totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
            totalPrice = items.stream()
                    .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                    .sum();
        } else {
            totalItems = 0;
            totalPrice = 0.0;
        }
    }
}

