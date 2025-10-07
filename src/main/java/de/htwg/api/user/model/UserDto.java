package de.htwg.api.user.model;

import lombok.Builder;

@Builder
public record UserDto(Long id, String name, String email) {
}
