package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.UpdateCheckDto;
import one.epicsoft.vc.dto.UpdateCheckPackageDto;
import one.epicsoft.vc.dto.VersionPreviewDto;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import one.epicsoft.vc.jpa.entity.UpdateCheckPackageEntity;
import one.epicsoft.vc.version.VersionCandidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UpdateCheckMapper {

  @Mapping(target = "packages", ignore = true)
  UpdateCheckDto toDto(final UpdateCheckEntity entity);

  UpdateCheckPackageDto toPackageDto(final UpdateCheckPackageEntity entity);

  List<UpdateCheckPackageDto> toPackageDtos(final List<UpdateCheckPackageEntity> entities);

  VersionPreviewDto.Candidate toCandidate(final VersionCandidate candidate);

  List<VersionPreviewDto.Candidate> toCandidates(final List<VersionCandidate> candidates);
}
