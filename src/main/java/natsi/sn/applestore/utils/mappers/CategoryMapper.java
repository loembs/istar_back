package natsi.sn.applestore.utils.mappers;

import natsi.sn.applestore.data.models.Category;
import natsi.sn.applestore.web.dto.CategoryDto;

public interface CategoryMapper {
    CategoryDto toDto(Category category);
}
