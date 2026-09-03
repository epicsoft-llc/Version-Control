package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.ProjectDto;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

  ProjectDto toDto(final ProjectEntity entity);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "name", source = "name")
  @Mapping(target = "description", source = "description")
  @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
  void updateEntity(@MappingTarget final ProjectEntity entity, final ProjectRequest request);
}
