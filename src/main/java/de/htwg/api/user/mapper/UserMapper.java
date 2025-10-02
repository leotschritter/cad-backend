package de.htwg.api.user.mapper;

import de.htwg.api.user.model.UserDto;
import de.htwg.persistence.entity.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserMapper {

    public User toEntity(UserDto dto) {
        return User.builder()
                .name(dto.name())
                .email(dto.email())
                .build();
    }

    public UserDto toDto(User entity) {
        return UserDto.builder()
                .name(entity.getName())
                .email(entity.getEmail())
                .build();
    }
}
