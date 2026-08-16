package com.ma_fashion_vibe_be.mapper;

import com.ma_fashion_vibe_be.dto.user.UserResponse;
import com.ma_fashion_vibe_be.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "provider", expression = "java(user.getProvider() != null ? user.getProvider().name() : null)")
    UserResponse toUserResponse(User user);
}
