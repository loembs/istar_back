package natsi.sn.applestore.data.repository;

import natsi.sn.applestore.data.models.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {
    @Query("SELECT c FROM ProductColor c WHERE c.product.id = :productId")
    List<ProductColor> findByProductId(@Param("productId") Integer productId);
}
