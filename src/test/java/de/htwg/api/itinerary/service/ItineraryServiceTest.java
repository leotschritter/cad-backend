package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.mapper.ItineraryMapper;
import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;
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
    private ItinerarySearchResponseDto testSearchResponseDto;

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

        testSearchResponseDto = ItinerarySearchResponseDto.builder()
                .title("Family Trip to Norway")
                .destination("Norway")
                .startDate(LocalDate.of(2024, 6, 15))
                .shortDescription("Explore the fjords of southern Norway")
                .detailedDescription("A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord.")
                .userName("Test User")
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
        assertEquals(testItineraryDto, result.getFirst());
        
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

    @Test
    void testSearchItinerariesByDestination() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .destination("Norway")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, null, null, "Norway", null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, null, null, "Norway", null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesByUserName() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .userName("Test User")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                "Test User", null, null, null, null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                "Test User", null, null, null, null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesByUserEmail() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .userEmail("test@example.com")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, "test@example.com", null, null, null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, "test@example.com", null, null, null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesByTitle() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .title("Family Trip")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, null, "Family Trip", null, null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, null, "Family Trip", null, null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesByDescription() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .description("fjords")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, null, null, null, "fjords", null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, null, null, null, "fjords", null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesByDateRange() {
        // Given
        LocalDate startFrom = LocalDate.of(2024, 6, 1);
        LocalDate startTo = LocalDate.of(2024, 12, 31);

        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .startDateFrom(startFrom)
                .startDateTo(startTo)
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, null, null, null, null, startFrom, startTo
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, null, null, null, null, startFrom, startTo
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesWithMultipleCriteria() {
        // Given
        LocalDate startFrom = LocalDate.of(2024, 1, 1);

        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .userName("Test")
                .destination("Norway")
                .description("fjords")
                .startDateFrom(startFrom)
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                "Test", null, null, "Norway", "fjords", startFrom, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                "Test", null, null, "Norway", "fjords", startFrom, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesWithAllCriteria() {
        // Given
        LocalDate startFrom = LocalDate.of(2024, 6, 1);
        LocalDate startTo = LocalDate.of(2024, 12, 31);

        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .userName("Test User")
                .userEmail("test@example.com")
                .title("Family Trip")
                .destination("Norway")
                .description("fjords")
                .startDateFrom(startFrom)
                .startDateTo(startTo)
                .build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                "Test User", "test@example.com", "Family Trip", "Norway", "fjords", startFrom, startTo
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                "Test User", "test@example.com", "Family Trip", "Norway", "fjords", startFrom, startTo
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesWithEmptyCriteria() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder().build();

        List<Itinerary> itineraries = List.of(testItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto);

        when(itineraryRepository.searchItineraries(
                null, null, null, null, null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSearchResponseDto, result.getFirst());

        verify(itineraryRepository).searchItineraries(
                null, null, null, null, null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }

    @Test
    void testSearchItinerariesReturnsEmptyList() {
        // Given
        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .destination("NonExistentPlace")
                .build();

        when(itineraryRepository.searchItineraries(
                null, null, null, "NonExistentPlace", null, null, null
        )).thenReturn(List.of());
        when(itineraryMapper.toSearchResponseDtoList(List.of())).thenReturn(List.of());

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(itineraryRepository).searchItineraries(
                null, null, null, "NonExistentPlace", null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(List.of());
    }

    @Test
    void testSearchItinerariesReturnsMultipleResults() {
        // Given
        User secondUser = User.builder()
                .id(2L)
                .name("Second User")
                .email("second@example.com")
                .build();

        Itinerary secondItinerary = Itinerary.builder()
                .id(2L)
                .title("Summer in Norway")
                .destination("Norway")
                .startDate(LocalDate.of(2025, 7, 1))
                .shortDescription("Summer vacation")
                .detailedDescription("Hiking and camping in Norwegian mountains.")
                .user(secondUser)
                .build();

        ItinerarySearchResponseDto secondDto = ItinerarySearchResponseDto.builder()
                .title("Summer in Norway")
                .destination("Norway")
                .startDate(LocalDate.of(2025, 7, 1))
                .shortDescription("Summer vacation")
                .detailedDescription("Hiking and camping in Norwegian mountains.")
                .userName("Second User")
                .build();

        ItinerarySearchDto searchDto = ItinerarySearchDto.builder()
                .destination("Norway")
                .build();

        List<Itinerary> itineraries = List.of(testItinerary, secondItinerary);
        List<ItinerarySearchResponseDto> expectedDtos = List.of(testSearchResponseDto, secondDto);

        when(itineraryRepository.searchItineraries(
                null, null, null, "Norway", null, null, null
        )).thenReturn(itineraries);
        when(itineraryMapper.toSearchResponseDtoList(itineraries)).thenReturn(expectedDtos);

        // When
        List<ItinerarySearchResponseDto> result = itineraryService.searchItineraries(searchDto);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testSearchResponseDto));
        assertTrue(result.contains(secondDto));

        verify(itineraryRepository).searchItineraries(
                null, null, null, "Norway", null, null, null
        );
        verify(itineraryMapper).toSearchResponseDtoList(itineraries);
    }
}
