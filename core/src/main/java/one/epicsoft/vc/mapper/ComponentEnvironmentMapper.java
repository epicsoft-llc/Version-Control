package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.ComponentEnvironmentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentPackageDto;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentEntity;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentPackageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ComponentEnvironmentMapper {

  @Mapping(target = "packages", ignore = true)
  @Mapping(target = "versionUnknown", expression = "java(entity.isVersionUnknown())")
  @Mapping(target = "movingTag", expression = "java(entity.isMovingTag())")
  ComponentEnvironmentDto toDto(final ComponentEnvironmentEntity entity);

  ComponentEnvironmentPackageDto toPackageDto(final ComponentEnvironmentPackageEntity entity);

  List<ComponentEnvironmentPackageDto> toPackageDtos(final List<ComponentEnvironmentPackageEntity> entities);
}
