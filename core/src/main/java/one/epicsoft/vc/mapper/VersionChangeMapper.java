package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.VersionChangeDto;
import one.epicsoft.vc.jpa.entity.VersionChangeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface VersionChangeMapper {

  /** Component and environment come along so a history list can name both. */
  @Mapping(target = "componentId", source = "componentEnvironment.componentId")
  @Mapping(target = "environmentId", source = "componentEnvironment.environmentId")
  VersionChangeDto toDto(final VersionChangeEntity entity);

  List<VersionChangeDto> toDtos(final List<VersionChangeEntity> entities);
}
