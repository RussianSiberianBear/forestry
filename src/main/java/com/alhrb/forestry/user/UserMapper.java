package com.alhrb.forestry.user;

import com.alhrb.forestry.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "__clientId", target = "clientId")
    UserDto toDto(User user);

    @Mapping(source = "clientId", target = "__clientId")
    User toEntity(UserDto dto);

    default Integer map(UserRole role) {
        return role == null ? null : role.ordinal();
    }

    default UserRole map(Integer index) {
        return index == null ? null : UserRole.getByIndex(index);
    }
}