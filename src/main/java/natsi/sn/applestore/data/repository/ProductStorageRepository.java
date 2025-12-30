package natsi.sn.applestore.data.repository;

import natsi.sn.applestore.data.models.ProductStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductStorageRepository extends JpaRepository<ProductStorage, Long> {
    @Query("SELECT s FROM ProductStorage s WHERE s.product.id = :productId")
    List<ProductStorage> findByProductId(@Param("productId") Integer productId);
}
