package de.htwg.api.user.service;

import de.htwg.api.user.mapper.UserMapper;
import de.htwg.api.user.model.UserDto;
import de.htwg.persistence.entity.User;
import de.htwg.persistence.repository.UserRepository;
import de.htwg.service.storage.ImageStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ImageStorageService imageStorageService;

    @Inject
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, ImageStorageService imageStorageService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.imageStorageService = imageStorageService;
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

    @Override
    @Transactional
    public void updateProfileImage(String email, String imageUrl) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        
        if (userOptional.isEmpty()) {
            // Auto-create user if they don't exist (they're authenticated via Identity Platform)
            String name = email.split("@")[0];
            user = User.builder()
                    .email(email)
                    .name(name)
                    .build();
            userRepository.persist(user);
            log.info("Auto-created user for profile image update: " + email);
        } else {
            user = userOptional.get();
        }

        log.info("Updating profile image for user: " + user.getEmail());
        user.setProfileImageUrl(imageUrl);
        userRepository.persist(user);
    }

    @Override
    @Transactional
    public String getProfileImageUrl(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        
        if (userOptional.isEmpty()) {
            // Auto-create user if they don't exist (they're authenticated via Identity Platform)
            String name = email.split("@")[0];
            user = User.builder()
                    .email(email)
                    .name(name)
                    .build();
            userRepository.persist(user);
            log.info("Auto-created user for profile image retrieval: " + email);
            return null; // New user has no profile image yet
        } else {
            user = userOptional.get();
        }

        String imageUrl = user.getProfileImageUrl();
        
        if (imageUrl == null) {
            return null;
        }
        
        return imageStorageService.getImageUrl(imageUrl);
    }
}
