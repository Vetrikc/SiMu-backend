package org.example.demo2.mapper;

import org.example.demo2.dto.UserInfoResponse;
import org.example.demo2.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between User entity and DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Converts User entity to UserInfoResponse DTO.
     *
     * @param user The User entity.
     * @return UserInfoResponse DTO.
     */
    @Mapping(target = "username", source = "username")
    UserInfoResponse toUserInfoResponse(User user);
}