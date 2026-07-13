package com.alhrb.forestry.user;

import com.alhrb.forestry.dto.UserDto;
import org.mapstruct.Mapper;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface UserMapper {

    Map<String, Object> toDto(User user);

    User toEntity(UserDto dto);
}