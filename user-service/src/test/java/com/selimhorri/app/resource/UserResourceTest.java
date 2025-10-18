package com.selimhorri.app.resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.UserService;

@WebMvcTest(UserResource.class)
class UserResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUserDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() throws Exception {
        // Given
        List<UserDto> users = Arrays.asList(testUserDto);
        DtoCollectionResponse<UserDto> response = new DtoCollectionResponse<>(users);
        when(userService.findAll()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].firstName").value("John"))
                .andExpect(jsonPath("$.collection[0].lastName").value("Doe"));

        verify(userService).findAll();
    }

    @Test
    void testFindById_ShouldReturnUser() throws Exception {
        // Given
        Integer userId = 1;
        when(userService.findById(userId)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(userService).findById(userId);
    }

    @Test
    void testSave_ShouldCreateUser() throws Exception {
        // Given
        when(userService.save(any(UserDto.class))).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService).save(any(UserDto.class));
    }

    @Test
    void testUpdate_ShouldUpdateUser() throws Exception {
        // Given
        when(userService.update(any(UserDto.class))).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(put("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService).update(any(UserDto.class));
    }

    @Test
    void testUpdateById_ShouldUpdateUser() throws Exception {
        // Given
        Integer userId = 1;
        when(userService.update(eq(userId), any(UserDto.class))).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService).update(eq(userId), any(UserDto.class));
    }

    @Test
    void testDeleteById_ShouldDeleteUser() throws Exception {
        // Given
        Integer userId = 1;
        doNothing().when(userService).deleteById(userId);

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userService).deleteById(userId);
    }

    @Test
    void testFindByUsername_ShouldReturnUser() throws Exception {
        // Given
        String username = "johndoe";
        when(userService.findByUsername(username)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService).findByUsername(username);
    }
}
