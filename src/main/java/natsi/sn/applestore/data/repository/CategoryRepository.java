package natsi.sn.applestore.data.repository;

import natsi.sn.applestore.data.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long>
{
}
