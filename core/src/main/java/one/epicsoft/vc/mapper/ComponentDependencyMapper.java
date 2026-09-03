package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.ComponentDependencyDto;
import one.epicsoft.vc.jpa.entity.ComponentDependencyEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ComponentDependencyMapper {

  ComponentDependencyDto toDto(final ComponentDependencyEntity entity);

  List<ComponentDependencyDto> toDtos(final List<ComponentDependencyEntity> entities);
}
