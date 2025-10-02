package de.htwg.api.user.service;

import de.htwg.api.user.mapper.UserMapper;
import de.htwg.api.user.model.UserDto;
import de.htwg.persistence.entity.User;
import de.htwg.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Inject
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserDto registerUser(UserDto userDto) {
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(userDto.email());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with email " + userDto.email() + " already exists");
        }

        // Create new user
        User user = userMapper.toEntity(userDto);
        userRepository.persist(user);
        
        return userMapper.toDto(user);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found");
        }
        
        return userMapper.toDto(user.get());
    }
}
