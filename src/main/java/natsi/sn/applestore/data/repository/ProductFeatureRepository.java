package natsi.sn.applestore.data.repository;

import natsi.sn.applestore.data.models.ProductFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductFeatureRepository extends JpaRepository<ProductFeature, Long> {
    @Query("SELECT f FROM ProductFeature f WHERE f.product.id = :productId")
    List<ProductFeature> findByProductId(@Param("productId") Integer productId);
}
