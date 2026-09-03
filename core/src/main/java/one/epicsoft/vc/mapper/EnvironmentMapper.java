package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.jpa.entity.EnvironmentEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnvironmentMapper {

  EnvironmentDto toDto(final EnvironmentEntity entity);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "name", source = "name")
  @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
  @Mapping(target = "production", source = "production", defaultValue = "false")
  void updateEntity(@MappingTarget final EnvironmentEntity entity, final EnvironmentRequest request);
}
