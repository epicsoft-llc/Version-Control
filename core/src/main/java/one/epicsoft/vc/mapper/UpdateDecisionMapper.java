package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.UpdateDecisionDto;
import one.epicsoft.vc.jpa.entity.UpdateDecisionEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UpdateDecisionMapper {

  UpdateDecisionDto toDto(final UpdateDecisionEntity entity);

  List<UpdateDecisionDto> toDtos(final List<UpdateDecisionEntity> entities);
}
