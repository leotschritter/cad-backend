package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.mapper.ItineraryMapper;
import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.User;
import de.htwg.persistence.repository.ItineraryRepository;
import de.htwg.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItineraryMapper itineraryMapper;

    @InjectMocks
    private ItineraryServiceImpl itineraryService;

    private Long testUserId;
    private User testUser;
    private ItineraryDto testItineraryDto;
    private Itinerary testItinerary;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .build();

        testItineraryDto = ItineraryDto.builder()
                .title("Family Trip to Norway")
                .destination("Norway")
                .startDate(LocalDate.of(2024, 6, 15))
                .shortDescription("Explore the fjords of southern Norway")
                .detailedDescription("A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord.")
                .build();

        testItinerary = Itinerary.builder()
                .id(1L)
                .title("Family Trip to Norway")
                .destination("Norway")
                .startDate(LocalDate.of(2024, 6, 15))
                .shortDescription("Explore the fjords of southern Norway")
                .detailedDescription("A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord.")
                .user(testUser)
                .build();
    }

    @Test
    void testCreateItinerary() {
        // Given
        when(userRepository.findByIdOptional(testUserId)).thenReturn(Optional.of(testUser));
        when(itineraryMapper.toEntity(testItineraryDto, testUser)).thenReturn(testItinerary);

        // When
        itineraryService.createItinerary(testItineraryDto, testUserId);

        // Then
        verify(userRepository).findByIdOptional(testUserId);
        verify(itineraryMapper).toEntity(testItineraryDto, testUser);
        verify(itineraryRepository).persist(testItinerary);
    }

    @Test
    void testCreateItineraryWithNonExistentUser() {
        // Given
        when(userRepository.findByIdOptional(testUserId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> itineraryService.createItinerary(testItineraryDto, testUserId));
        
        assertEquals("User with id " + testUserId + " not found", exception.getMessage());
        
        verify(userRepository).findByIdOptional(testUserId);
        verify(itineraryMapper, never()).toEntity(any(), any());
        verify(itineraryRepository, never()).persist((Itinerary) any());
    }

    @Test
    void testGetItinerariesByUserId() {
        // Given
        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItineraryDto> expectedDtos = List.of(testItineraryDto);
        
        when(itineraryRepository.findByUserId(testUserId)).thenReturn(itineraries);
        when(itineraryMapper.toDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItineraryDto> result = itineraryService.getItinerariesByUserId(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testItineraryDto, result.get(0));
        
        verify(itineraryRepository).findByUserId(testUserId);
        verify(itineraryMapper).toDtoList(itineraries);
    }

    @Test
    void testGetItinerariesForNonExistentUser() {
        // Given
        when(itineraryRepository.findByUserId(999L)).thenReturn(List.of());
        when(itineraryMapper.toDtoList(List.of())).thenReturn(List.of());

        // When
        List<ItineraryDto> result = itineraryService.getItinerariesByUserId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(itineraryRepository).findByUserId(999L);
        verify(itineraryMapper).toDtoList(List.of());
    }
}
