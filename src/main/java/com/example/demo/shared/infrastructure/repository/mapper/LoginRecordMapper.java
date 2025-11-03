package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.shared.infrastructure.repository.data.LoginRecordData;
import com.example.demo.user.domain.model.LoginRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginRecordMapper {

    LoginRecord toDomain(LoginRecordData data);

    LoginRecordData toData(LoginRecord domain);
}
