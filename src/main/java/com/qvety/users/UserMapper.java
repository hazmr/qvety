package com.qvety.users;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserMapper {

    UserDto toDto(User user);

    /** Only the editable columns. Never id, practiceId, email, passwordHash, sessionVersion, timestamps. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "veterinarian", source = "veterinarian")
    @Mapping(target = "licenseNumber", source = "licenseNumber")
    @Mapping(target = "phone", source = "phone")
    void updateFromRequest(UserUpdateRequest request, @MappingTarget User user);
}
