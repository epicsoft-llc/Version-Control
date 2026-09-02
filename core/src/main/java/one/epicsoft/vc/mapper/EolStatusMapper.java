package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.EolStatusDto;
import one.epicsoft.vc.jpa.entity.EolStatusEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EolStatusMapper {

  EolStatusDto toDto(final EolStatusEntity entity);

  List<EolStatusDto> toDtos(final List<EolStatusEntity> entities);
}
