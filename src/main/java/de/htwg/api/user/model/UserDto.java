package de.htwg.api.user.model;

import lombok.Builder;

@Builder
public record UserDto(String name, String email) {
}
