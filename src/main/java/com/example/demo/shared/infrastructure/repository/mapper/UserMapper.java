package com.example.demo.shared.infrastructure.repository.mapper;

import com.example.demo.shared.infrastructure.repository.data.UserData;
import com.example.demo.user.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserData userData);

    UserData toData(User user);
}
