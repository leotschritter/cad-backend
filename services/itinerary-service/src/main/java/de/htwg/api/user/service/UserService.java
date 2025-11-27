package de.htwg.api.user.service;

import de.htwg.api.user.model.UserDto;

public interface UserService {

    UserDto registerUser(UserDto userDto);

    UserDto getUserByEmail(String email);

    void updateProfileImage(String email, String imageUrl);

    String getProfileImageUrl(String email);
}
