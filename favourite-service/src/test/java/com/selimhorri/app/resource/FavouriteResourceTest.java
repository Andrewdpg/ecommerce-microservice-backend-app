package com.selimhorri.app.resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
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
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.FavouriteService;

@WebMvcTest(FavouriteResource.class)
class FavouriteResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavouriteService favouriteService;

    @Autowired
    private ObjectMapper objectMapper;

    private FavouriteDto testFavouriteDto;

    @BeforeEach
    void setUp() {
        testFavouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllFavourites() throws Exception {
        // Given
        List<FavouriteDto> favourites = Arrays.asList(testFavouriteDto);
        DtoCollectionResponse<FavouriteDto> response = new DtoCollectionResponse<>(favourites);
        when(favouriteService.findAll()).thenReturn(favourites);

        // When & Then
        mockMvc.perform(get("/api/favourites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1));

        verify(favouriteService).findAll();
    }

    @Test
    void testFindById_ShouldReturnFavourite() throws Exception {
        // Given
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456);
        when(favouriteService.findById(any(FavouriteId.class))).thenReturn(testFavouriteDto);

        // When & Then
        String likeDateStr = testDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy__HH:mm:ss:SSSSSS"));
        mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}", 
                "1", "1", likeDateStr))
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("Response body: " + result.getResponse().getContentAsString());
                })
                .andExpect(jsonPath("$.productId").value(1));

        verify(favouriteService).findById(any(FavouriteId.class));
    }

    @Test
    void testSave_ShouldCreateFavourite() throws Exception {
        // Given
        when(favouriteService.save(any(FavouriteDto.class))).thenReturn(testFavouriteDto);

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFavouriteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.likeDate").exists());

        verify(favouriteService).save(any(FavouriteDto.class));
    }

    @Test
    void testUpdate_ShouldUpdateFavourite() throws Exception {
        // Given
        when(favouriteService.update(any(FavouriteDto.class))).thenReturn(testFavouriteDto);

        // When & Then
        mockMvc.perform(put("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFavouriteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.likeDate").exists());

        verify(favouriteService).update(any(FavouriteDto.class));
    }

    @Test
    void testUpdateById_ShouldUpdateFavourite() throws Exception {
        // Given
        when(favouriteService.update(any(FavouriteDto.class))).thenReturn(testFavouriteDto);

        // When & Then
        mockMvc.perform(put("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFavouriteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.likeDate").exists());

        verify(favouriteService).update(any(FavouriteDto.class));
    }

    @Test
    void testDeleteById_ShouldDeleteFavourite() throws Exception {
        // Given
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456);
        doNothing().when(favouriteService).deleteById(any(FavouriteId.class));

        // When & Then
        String likeDateStr = testDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy__HH:mm:ss:SSSSSS"));
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}/{likeDate}", 
                "1", "1", likeDateStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(favouriteService).deleteById(any(FavouriteId.class));
    }

    @Test
    void testSave_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        FavouriteDto invalidFavouriteDto = FavouriteDto.builder()
                .userId(null)
                .productId(null)
                .likeDate(null)
                .build();

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidFavouriteDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSave_WithValidFavourite_ShouldSucceed() throws Exception {
        // Given
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(2)
                .productId(2)
                .likeDate(LocalDateTime.of(2024, 2, 20, 15, 45, 30, 789012))
                .build();

        FavouriteDto savedFavourite = FavouriteDto.builder()
                .userId(2)
                .productId(2)
                .likeDate(LocalDateTime.of(2024, 2, 20, 15, 45, 30, 789012))
                .build();

        when(favouriteService.save(any(FavouriteDto.class))).thenReturn(savedFavourite);

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.likeDate").exists());

        verify(favouriteService).save(any(FavouriteDto.class));
    }
}
