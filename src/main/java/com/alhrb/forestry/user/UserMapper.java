package com.alhrb.forestry.user;

import com.alhrb.forestry.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(UserDto dto);

    default Integer map(UserRole role) {
        return role == null ? null : role.ordinal();
    }

    default UserRole map(Integer index) {
        return index == null ? null : UserRole.getByIndex(index);
    }
}