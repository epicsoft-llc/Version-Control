package one.epicsoft.vc.mapper;

import one.epicsoft.vc.dto.UpgradeStopDto;
import one.epicsoft.vc.jpa.entity.UpgradeStopEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UpgradeStopMapper {

  UpgradeStopDto toDto(final UpgradeStopEntity entity);

  List<UpgradeStopDto> toDtos(final List<UpgradeStopEntity> entities);
}
