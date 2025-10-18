package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;
    private Credential testCredential;
    private CredentialDto testCredentialDto;

    @BeforeEach
    void setUp() {
        testCredential = Credential.builder()
                .credentialId(1)
                .username("johndoe")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testCredentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("johndoe")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testUser = User.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .credential(testCredential)
                .build();

        testUserDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .credentialDto(testCredentialDto)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(userRepository).findAll();
    }

    @Test
    void testFindById_WhenUserExists_ShouldReturnUser() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("John", result.getFirstName());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindById_WhenUserNotExists_ShouldThrowException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserObjectNotFoundException exception = assertThrows(
                UserObjectNotFoundException.class,
                () -> userService.findById(userId)
        );
        
        assertTrue(exception.getMessage().contains("User with id: 999 not found"));
        verify(userRepository).findById(userId);
    }

    @Test
    void testSave_ShouldReturnSavedUser() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.save(testUserDto);

        // Then
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(testUserDto.getFirstName(), result.getFirstName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdate_ShouldReturnUpdatedUser() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.update(testUserDto);

        // Then
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteById_ShouldCallRepositoryDelete() {
        // Given
        Integer userId = 1;
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteById(userId);

        // Then
        verify(userRepository).deleteById(userId);
    }

    @Test
    void testFindByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        String username = "johndoe";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository).findByCredentialUsername(username);
    }

    @Test
    void testFindByUsername_WhenUserNotExists_ShouldThrowException() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.empty());

        // When & Then
        UserObjectNotFoundException exception = assertThrows(
                UserObjectNotFoundException.class,
                () -> userService.findByUsername(username)
        );
        
        assertTrue(exception.getMessage().contains("User with username: nonexistent not found"));
        verify(userRepository).findByCredentialUsername(username);
    }
}
