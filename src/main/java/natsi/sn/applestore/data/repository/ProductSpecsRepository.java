package natsi.sn.applestore.data.repository;

import natsi.sn.applestore.data.models.ProductSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSpecsRepository extends JpaRepository<ProductSpecs, Long> {
    @Query("SELECT s FROM ProductSpecs s WHERE s.product.id = :productId")
    List<ProductSpecs> findByProductId(@Param("productId") Integer productId);
}
