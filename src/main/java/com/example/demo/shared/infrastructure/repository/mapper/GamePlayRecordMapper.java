package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.game.domain.model.GamePlayRecord;
import com.example.demo.shared.infrastructure.repository.data.GamePlayRecordData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GamePlayRecordMapper {

    GamePlayRecord toDomain(GamePlayRecordData data);

    GamePlayRecordData toData(GamePlayRecord domain);
}
