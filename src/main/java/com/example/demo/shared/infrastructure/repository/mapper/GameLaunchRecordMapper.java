package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.game.domain.model.GameLaunchRecord;
import com.example.demo.shared.infrastructure.repository.data.GameLaunchRecordData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameLaunchRecordMapper {

    GameLaunchRecord toDomain(GameLaunchRecordData data);

    GameLaunchRecordData toData(GameLaunchRecord domain);
}
