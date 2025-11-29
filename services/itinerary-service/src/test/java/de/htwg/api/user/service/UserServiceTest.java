package de.htwg.api.user.service;

import de.htwg.api.user.mapper.UserMapper;
import de.htwg.api.user.model.UserDto;
import de.htwg.persistence.entity.User;
import de.htwg.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDto testUserDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserDto = UserDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Test
    void testRegisterUser() {
        // Given
        when(userRepository.findByEmail(testUserDto.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(testUserDto)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        // When
        UserDto result = userService.registerUser(testUserDto);

        // Then
        assertNotNull(result);
        assertEquals(testUserDto.name(), result.name());
        assertEquals(testUserDto.email(), result.email());

        verify(userRepository).findByEmail(testUserDto.email());
        verify(userMapper).toEntity(testUserDto);
        verify(userRepository).persist(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void testRegisterUserWithExistingEmail() {
        // Given
        when(userRepository.findByEmail(testUserDto.email())).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(testUserDto));

        assertEquals("User with email " + testUserDto.email() + " already exists", exception.getMessage());

        verify(userRepository).findByEmail(testUserDto.email());
        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).persist((User) any());
    }

    @Test
    void testGetUserByEmail() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        // When
        UserDto result = userService.getUserByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(testUserDto.name(), result.name());
        assertEquals(testUserDto.email(), result.email());

        verify(userRepository).findByEmail(email);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void testGetUserByEmailNotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserByEmail(email));

        assertEquals("User with email " + email + " not found", exception.getMessage());

        verify(userRepository).findByEmail(email);
        verify(userMapper, never()).toDto(any());
    }
}
