package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.impl.FavouriteServiceImpl;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    private Favourite testFavourite;
    private FavouriteDto testFavouriteDto;

    @BeforeEach
    void setUp() {
        testFavourite = Favourite.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .userId(1)
                .productId(1)
                .build();

        testFavouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .userId(1)
                .productId(1)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllFavourites() {
        // Given
        List<Favourite> favourites = Arrays.asList(testFavourite);
        when(favouriteRepository.findAll()).thenReturn(favourites);

        // When
        List<FavouriteDto> result = favouriteService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getUserId());
        verify(favouriteRepository).findAll();
    }

    @Test
    void testFindById_WhenFavouriteExists_ShouldReturnFavourite() {
        // Given
        FavouriteId favouriteId = new FavouriteId(1, 1, LocalDateTime.now());
        when(favouriteRepository.findById(favouriteId)).thenReturn(Optional.of(testFavourite));

        // When
        FavouriteDto result = favouriteService.findById(favouriteId);

        // Then
        assertNotNull(result);
        assertEquals(favouriteId.getUserId(), result.getUserId());
        assertEquals(Integer.valueOf(1), result.getUserId());
        verify(favouriteRepository).findById(favouriteId);
    }

    @Test
    void testFindById_WhenFavouriteNotExists_ShouldThrowException() {
        // Given
        FavouriteId favouriteId = new FavouriteId(999, 999, LocalDateTime.now());
        when(favouriteRepository.findById(favouriteId)).thenReturn(Optional.empty());

        // When & Then
        FavouriteNotFoundException exception = assertThrows(
                FavouriteNotFoundException.class,
                () -> favouriteService.findById(favouriteId)
        );
        
        assertTrue(exception.getMessage().contains("Favourite with id: [" + favouriteId + "] not found!"));
        verify(favouriteRepository).findById(favouriteId);
    }

    @Test
    void testSave_ShouldReturnSavedFavourite() {
        // Given
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);

        // When
        FavouriteDto result = favouriteService.save(testFavouriteDto);

        // Then
        assertNotNull(result);
        assertEquals(testFavouriteDto.getUserId(), result.getUserId());
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    void testUpdate_ShouldReturnUpdatedFavourite() {
        // Given
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);

        // When
        FavouriteDto result = favouriteService.update(testFavouriteDto);

        // Then
        assertNotNull(result);
        assertEquals(testFavouriteDto.getUserId(), result.getUserId());
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    void testDeleteById_ShouldCallRepositoryDelete() {
        // Given
        FavouriteId favouriteId = new FavouriteId(1, 1, LocalDateTime.now());
        doNothing().when(favouriteRepository).deleteById(favouriteId);

        // When
        favouriteService.deleteById(favouriteId);

        // Then
        verify(favouriteRepository).deleteById(favouriteId);
    }

    @Test
    void testSave_WithValidData_ShouldSucceed() {
        // Given
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(2)
                .productId(2)
                .build();

        Favourite savedFavourite = Favourite.builder()
                .userId(2)
                .productId(2)
                .likeDate(LocalDateTime.now())
                .userId(2)
                .productId(2)
                .build();

        when(favouriteRepository.save(any(Favourite.class))).thenReturn(savedFavourite);

        // When
        FavouriteDto result = favouriteService.save(newFavourite);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getUserId());
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    void testUpdate_WithValidData_ShouldSucceed() {
        // Given
        FavouriteDto updatedFavourite = FavouriteDto.builder()
                .userId(1)
                .productId(3)
                .likeDate(LocalDateTime.now())
                .userId(1)
                .productId(3)
                .build();

        Favourite savedFavourite = Favourite.builder()
                .userId(1)
                .productId(3)
                .likeDate(LocalDateTime.now())
                .userId(1)
                .productId(3)
                .build();

        when(favouriteRepository.save(any(Favourite.class))).thenReturn(savedFavourite);

        // When
        FavouriteDto result = favouriteService.update(updatedFavourite);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(3), result.getProductId());
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    void testUpdate_WithFavouriteId_ShouldSucceed() {
        // Given
        FavouriteId favouriteId = new FavouriteId(1, 4, LocalDateTime.now());
        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(4)
                .build();

        Favourite savedFavourite = Favourite.builder()
                .userId(1)
                .productId(4)
                .likeDate(LocalDateTime.now())
                .userId(1)
                .productId(4)
                .likeDate(LocalDateTime.now())
                .build();

        when(favouriteRepository.save(any(Favourite.class))).thenReturn(savedFavourite);

        // When
        FavouriteDto result = favouriteService.update(favouriteDto);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(4), result.getProductId());
        verify(favouriteRepository).save(any(Favourite.class));
    }
}
