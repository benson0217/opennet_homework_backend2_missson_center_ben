package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.game.domain.model.Game;
import com.example.demo.shared.infrastructure.repository.data.GameData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameMapper {

    Game toDomain(GameData data);

    GameData toData(Game domain);
}
