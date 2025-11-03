package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.shared.infrastructure.repository.data.MissionData;
import com.example.demo.mission.domain.model.Mission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MissionPersistenceMapper {

    Mission toDomain(MissionData data);

    MissionData toData(Mission domain);
}
