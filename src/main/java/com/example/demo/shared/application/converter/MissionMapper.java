package com.example.demo.shared.application.converter;

import com.example.demo.shared.application.dto.MissionResponse;
import com.example.demo.mission.domain.model.Mission;
import com.example.demo.mission.domain.model.MissionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 任務 DTO 轉換器
 * 使用 MapStruct 進行 Mission 領域物件與 MissionResponse DTO 之間的轉換。
 */
@Mapper(componentModel = "spring")
public interface MissionMapper {

    /**
     * 將 Mission 領域物件轉換為 MissionResponse DTO。
     */
    @Mapping(source = "missionType", target = "missionType", qualifiedByName = "missionTypeToString")
    @Mapping(source = "missionType", target = "description", qualifiedByName = "missionTypeToDescription")
    @Mapping(source = "progressPercentage", target = "progressPercentage")
    MissionResponse toResponse(Mission mission);

    /**
     * 將 MissionType 枚舉轉換為字串。
     */
    @Named("missionTypeToString")
    default String missionTypeToString(MissionType missionType) {
        return missionType != null ? missionType.name() : null;
    }

    /**
     * 將 MissionType 枚舉轉換為其描述。
     */
    @Named("missionTypeToDescription")
    default String missionTypeToDescription(MissionType missionType) {
        return missionType != null ? missionType.getDescription() : null;
    }
}
