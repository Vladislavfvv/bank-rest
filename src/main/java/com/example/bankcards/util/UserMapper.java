package com.example.bankcards.util;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CardMapper.class})
public interface UserMapper {
    
    // User mappings
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "cardCount", expression = "java(user.getAllCards() != null ? user.getAllCards().size() : 0)")
    UserDto toDto(User user);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "cards", ignore = true) // Карты управляются отдельно
    User toEntity(UserDto dto);

    // Update method для пользователя
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "cards", ignore = true) // Карты обновляются отдельно
    @Mapping(target = "password", ignore = true) // Пароль обновляется отдельно
    void updateUserFromDto(UserDto dto, @MappingTarget User user);
}
