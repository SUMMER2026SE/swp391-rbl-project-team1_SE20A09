package com.sportvenue.mapper;

import com.sportvenue.dto.UserProfileResponse;
import com.sportvenue.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "roleName", source = "role.roleName")
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    UserProfileResponse toProfileResponse(User user);
}
